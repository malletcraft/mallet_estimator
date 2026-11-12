package com.malletcrafts.interiorestimate.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * The 2 · Mark palette (v52 slice 7), against ct52 section T ('palette types', 'Loft is in More ▸ now') and
 * FX8 ('Opening ▾' with U+202F). The '8 slots' check completes in slice 11, when Chain joins: here the slot
 * list is Select, the three types and More ▸, and the drawing tools slot in between when they exist.
 *
 * PORT 62 SLICE L6 (12 Nov 2026): More ▸ as one row per trade, the More slot's name, the tool chip's kind button and its
 * list of kinds are compared ROW FOR ROW with what the approved prototype's own code answers -- `src/test/resources/
 * palette/proto-palette-rows.txt`, the rows `android/docs/port62/proto-tables.mjs` prints between "L6 GOLDEN" and "L6
 * END" (T53.moreRows with renderMore's label and tiles; sp.js's own t53subBtn and t53subList, cut from the page and
 * run). The tool's remembered kind follows that script's "L6 the tool remembers" journey; the hand-written values are
 * typed from its output.
 */
class PaletteTest {

    private fun f(kind: String) = SurveyPrepPalette.paletteFor(kind).let { p ->
        p.main.joinToString("") { it.letter } + "/" + p.more.joinToString("") { it.letter } }

    private fun g(kind: String) = SurveyPrepPalette.paletteFor(kind).let { p ->
        p.main.joinToString(" ") { it.letter } + " / " + p.more.joinToString(" ") { it.letter } }

    @Test fun `palette types per kind, More in TYPES order`() {
        // ct52 T: walls W D E (More: C B O N L P S F X), floor C S E (More: O P X), ceiling B E P (More: C F O X) -- and
        // after them v53's ten where each may go (port 62 slice L5; index61c2.html 4926: TYPES53 pushed after v52's rows). The
        // three slots stay v52's: with no Trade chip on the phone the palette is paletteMain('all') (index61c2.html 2597)
        assertEquals("W D E / C B O N L P S F X NOTE GRL AC PLB CTR CRP LV SAN DEF", g("wall"))
        assertEquals("C S E / O P X NOTE PLB CRP SAN DEF", g("floor"))
        assertEquals("B E P / C F O X NOTE AC CDP CTR LV DEF", g("ceil"))
        assertEquals(g("wall"), SurveyPrepPalette.forSurface("left").let { p ->
            p.main.joinToString(" ") { it.letter } + " / " + p.more.joinToString(" ") { it.letter } })
        // v52's part, as ct52 T wrote it, is the head of each list
        assertTrue(f("wall").startsWith("WDE/CBONLPSFX")); assertTrue(f("floor").startsWith("CSE/OPX")); assertTrue(f("ceil").startsWith("BEP/CFOX"))
        assertEquals(listOf(21, 11, 13), listOf("wall", "floor", "ceil").map { SurveyPrepPalette.paletteFor(it).all.size })
    }

    @Test fun `the More sheet lists every type with its short name`() {
        // ct52 T: the sheet reads Window|Door|Column|Beam|Opening|Niche|Loft|Electric|Bulb|Switch|Fan|Other -- then the
        // ten's short names (T53 SHORT53, index61c2.html 2397-2407 and 2412), each fitting a slot
        assertEquals("Window|Door|Column|Beam|Opening|Niche|Loft|Electric|Bulb|Switch|Fan|Other|Note|Grill|AC|Plumbing|Track|Unit|LV point|Sanitary|Defect",
            SurveyPrepPalette.paletteFor("wall").all.joinToString("|") { SurveyPrepPalette.short(Tx.EN, it.name) })
        assertEquals("Column|Step|Opening|Electric|Bulb|Other|Note|Plumbing|Unit|Sanitary|Defect",
            SurveyPrepPalette.paletteFor("floor").all.joinToString("|") { SurveyPrepPalette.short(Tx.EN, it.name) })
        assertEquals("Beam|Column|Electric|Bulb|Fan|Opening|Other|Note|AC|Drop|Track|LV point|Defect",
            SurveyPrepPalette.paletteFor("ceil").all.joinToString("|") { SurveyPrepPalette.short(Tx.EN, it.name) })
        // a More type names itself on the More slot and on the tool chip by the same tables
        assertEquals("Unit\u202f▾", SurveyPrepPalette.moreLabel(Tx.EN, "wall", "CRP"))
        assertEquals("Drop\u202f▾", SurveyPrepPalette.moreLabel(Tx.EN, "ceil", "CDP"))
        assertEquals("Placing Carpentry unit · tap the photo", SurveyPrepPalette.chipText(Tx.EN, "front", "CRP"))
        assertEquals("Placing Note · tap the photo", SurveyPrepPalette.chipText(Tx.EN, "floor", "NOTE"))
        assertEquals("Every mark type · Front wall", SurveyPrepPalette.sheetTitle(Tx.EN, "front"))
        assertEquals("Every mark type · Ceiling", SurveyPrepPalette.sheetTitle(Tx.EN, "ceil"))
    }

    @Test fun `palette - exactly 8 slots, Select, Tape, Line, Chain, three types, More (ct52 T, v52 slice 11)`() {
        val now = SurveyPrepPalette.slots("wall")
        // ct52 T: "palette: exactly 8 slots, Select · Tape · Line · Chain · Window · Door · Electric · More ▸" -- never "Measure"
        assertEquals(8, now.size)
        assertEquals("Select|Tape|Line|Chain|Window|Door|Electric|More ▸", now.joinToString("|") { s ->
            when (s.kind) {
                SurveyPrepPalette.SlotKind.SELECT -> "Select"
                SurveyPrepPalette.SlotKind.MORE -> SurveyPrepPalette.moreLabel(Tx.EN, "wall", SurveyPrepPalette.SEL)
                SurveyPrepPalette.SlotKind.TOOL -> SurveyPrepPalette.toolName(Tx.EN, s.key)
                else -> SurveyPrepPalette.short(Tx.EN, s.type?.name ?: s.key)
            } })
        assertEquals(0xfacc15, SurveyPrepPalette.toolColour("T"))
        assertEquals(0x7dd3fc, SurveyPrepPalette.toolColour("M"))
        assertEquals(0xfb7185, SurveyPrepPalette.toolColour("K"))
        // Tape, Line and Chain all present, as tools, and every surface kind has its eight
        for (kind in listOf("wall", "floor", "ceil")) {
            val sl = SurveyPrepPalette.slots(kind)
            assertEquals(8, sl.size, kind)
            assertEquals(listOf("T", "M", "K"), sl.filter { it.kind == SurveyPrepPalette.SlotKind.TOOL }.map { it.key }, kind)
        }
        assertFalse(SurveyPrepPalette.isType("K")); assertNull(SurveyPrepPalette.chipText(Tx.EN, "front", "K"))
        assertTrue(SurveyPrepPalette.isMeasTool("T") && SurveyPrepPalette.isMeasTool("M")); assertFalse(SurveyPrepPalette.isMeasTool("K"))
        assertFalse(SurveyPrepPalette.isType("T")); assertFalse(SurveyPrepPalette.isType("M")); assertTrue(SurveyPrepPalette.isType("W"))
        assertNull(SurveyPrepPalette.chipText(Tx.EN, "front", "T")); assertNull(SurveyPrepPalette.moreArmed("wall", "M"))
        // slice 11's palette: Tape, Line, Chain between Select and the types -- exactly eight
        val v52 = SurveyPrepPalette.slots("wall", listOf("T", "M", "K"))
        assertEquals(8, v52.size)
        assertEquals("sel,T,M,K,W,D,E,", v52.joinToString(",") { it.key })
        assertEquals(SurveyPrepPalette.SlotKind.MORE, v52.last().kind)
        assertEquals(8, SurveyPrepPalette.slots("floor", listOf("T", "M", "K")).size)
        assertEquals(8, SurveyPrepPalette.slots("ceil", listOf("T", "M", "K")).size)
    }

    @Test fun `Loft is in More now, and More names it with a narrow no-break space`() {
        assertTrue(SurveyPrepPalette.paletteFor("wall").more.any { it.letter == "L" })
        assertFalse(SurveyPrepPalette.paletteFor("wall").main.any { it.letter == "L" })
        assertEquals("Loft ▾", SurveyPrepPalette.moreLabel(Tx.EN, "wall", "L"))
        // FX8: every More type names itself in full, "Opening ▾" too
        assertEquals("Opening ▾", SurveyPrepPalette.moreLabel(Tx.EN, "wall", "O"))
        assertEquals("Switch ▾", SurveyPrepPalette.moreLabel(Tx.EN, "wall", "S"))
        assertEquals("Bulb ▾", SurveyPrepPalette.moreLabel(Tx.EN, "floor", "P"))
        assertEquals("Column ▾", SurveyPrepPalette.moreLabel(Tx.EN, "ceil", "C"))
        // a main type, or Select, leaves it "More ▸"
        assertEquals("More ▸", SurveyPrepPalette.moreLabel(Tx.EN, "wall", "W"))
        assertEquals("More ▸", SurveyPrepPalette.moreLabel(Tx.EN, "wall", SurveyPrepPalette.SEL))
        // the floor's S is a Step, on the palette itself: More is not lit
        assertNull(SurveyPrepPalette.moreArmed("floor", "S"))
    }

    @Test fun `exactly one slot is lit`() {
        for ((kind, tool) in listOf("wall" to SurveyPrepPalette.SEL, "wall" to "D", "wall" to "L", "ceil" to "F", "floor" to "S", "wall" to "T", "floor" to "M", "ceil" to "K")) {
            val lit = SurveyPrepPalette.slots(kind).filter { SurveyPrepPalette.isOn(it, kind, tool) }
            assertEquals(1, lit.size, "$kind $tool")
        }
        assertEquals(SurveyPrepPalette.SlotKind.MORE,
            SurveyPrepPalette.slots("wall").single { SurveyPrepPalette.isOn(it, "wall", "N") }.kind)
    }

    @Test fun `tools are sticky and the one on goes back to Select`() {
        assertEquals("D", SurveyPrepPalette.next(SurveyPrepPalette.SEL, "D"))
        assertEquals("W", SurveyPrepPalette.next("D", "W"))
        assertEquals(SurveyPrepPalette.SEL, SurveyPrepPalette.next("D", "D"))
        assertEquals(SurveyPrepPalette.SEL, SurveyPrepPalette.next(SurveyPrepPalette.SEL, SurveyPrepPalette.SEL))
        assertEquals(SurveyPrepPalette.SEL, SurveyPrepPalette.next("D", SurveyPrepPalette.SEL))
    }

    @Test fun `the tool chip names the type, and Select shows none`() {
        assertNull(SurveyPrepPalette.chipText(Tx.EN, "front", SurveyPrepPalette.SEL))
        assertEquals("Placing Door · tap the photo", SurveyPrepPalette.chipText(Tx.EN, "front", "D"))
        assertEquals("Placing Switch plate · tap the photo", SurveyPrepPalette.chipText(Tx.EN, "left", "S"))
        assertEquals("Placing Step · tap the photo", SurveyPrepPalette.chipText(Tx.EN, "floor", "S"))
    }


    // ------------------------------------------------------------------ port 62 slice L4: "· as Client wish"

    private val STAGES: List<Int?> = listOf(0, 1, 2, 3, 4, 5, null)
    private fun chipAt(stage: Int?, tool: String = "E", where: String = "photo", picked: String? = null, tx: Tx = Tx.EN) =
        SurveyPrepPalette.chip(tx, "front", PhaseTools.toolFor(tool, stage), stage, picked, where)

    @Test fun `the chip per stage - none at Survey, plain words where the phase fixes it, the cycling button only while unread`() {
        // the page's own chip with Electric on, driven at each stage (v61c.2, 11 Nov 2026)
        assertEquals("Placing Electric point · tap the photo", chipAt(0)!!.text)
        assertEquals("Placing Electric point · tap the photo · as Client wish", chipAt(1)!!.text)
        assertEquals("Placing Electric point · tap the photo · as Vendor added", chipAt(2)!!.text)
        assertNull(chipAt(3), "Estimation has no palette: the tool is Select and no chip is drawn")
        assertEquals("Placing Electric point · tap the photo · as Vendor added", chipAt(4)!!.text)
        assertNull(chipAt(5), "nor has Client approved")
        assertEquals("Placing Electric point · tap the photo · as Existing ▾", chipAt(null)!!.text)
        // which face it is
        assertEquals(listOf("none", "plain", "plain", "-", "plain", "-", "button"), STAGES.map { st ->
            chipAt(st)?.let { if (it.asText == null) "none" else if (it.cycles) "button" else "plain" } ?: "-" })
        // Survey's chip is v52's own, to the letter: no second half, nothing to say for one
        chipAt(0)!!.let { c -> assertEquals(SurveyPrepPalette.chipText(Tx.EN, "front", "E"), c.text); assertEquals(-1, c.asFrom); assertNull(c.asLabel); assertEquals(c.main, c.spoken); assertFalse(c.cycles) }
        // a project S5 does not list is given Survey's stage: its chip is Survey's
        assertEquals(chipAt(0)!!.text, chipAt(PhaseTools.stageOf(null, phoneOnly = true))!!.text)
        // on the 360 the same second half
        assertEquals("Placing Window · tap the 360 · as Client wish", chipAt(1, "W", "360")!!.text)
        assertEquals("Placing Window · tap the 360", chipAt(0, "W", "360")!!.text)
        // Select and the drawing tools have other chips, in every phase
        for (st in STAGES) for (t in listOf(SurveyPrepPalette.SEL, "T", "M", "K")) assertNull(chipAt(st, t), "$t at $st")
        // a Note has no state: its chip has no second half (the page's `tool==='NOTE'?''`)
        assertNull(SurveyPrepPalette.chip(Tx.EN, "front", MarkLife.NOTE_TYPE, 1, null)!!.asText)
    }

    @Test fun `the chip's second half - where it starts, what is said for it, and that it is the phase's own word`() {
        val wish = chipAt(1)!!
        assertEquals("Placing Electric point · tap the photo", wish.main); assertEquals("as Client wish", wish.asText); assertFalse(wish.cycles)
        assertEquals("as Client wish", wish.text.substring(wish.asFrom)); assertEquals("Placing Electric point · tap the photo · ", wish.text.substring(0, wish.asFrom))
        assertEquals("Marks are placed as Client wish in this phase", wish.asLabel)
        assertEquals("Placing Electric point · tap the photo · Marks are placed as Client wish in this phase", wish.spoken)
        assertEquals("Marks are placed as Vendor added in this phase", chipAt(2)!!.asLabel); assertEquals(chipAt(2)!!.asLabel, chipAt(4)!!.asLabel)
        // where the phase fixes it, what this capture's button was once cycled to changes nothing
        for (picked in listOf(null, "exist", "wish", "vadd", "nonsense")) {
            assertEquals("as Client wish", chipAt(1, picked = picked)!!.asText, "$picked"); assertEquals("as Vendor added", chipAt(4, picked = picked)!!.asText, "$picked")
            assertNull(chipAt(0, picked = picked)!!.asText, "$picked at Survey")
        }
        // the words are what is PLACED: the chip and the placement read one rule
        for (st in STAGES) for (picked in listOf(null, "exist", "wish", "vadd")) chipAt(st, picked = picked)?.asText?.let { said ->
            assertEquals(SurveyPrepPalette.asName(Tx.EN, PhaseTools.asFor(st, picked)), said.removeSuffix(" ▾"), "$st $picked") }
        assertEquals(listOf("as Existing", "as Client wish", "as Vendor added", "as Existing", "as Existing"),
            listOf("exist", "wish", "vadd", null, "nonsense").map { SurveyPrepPalette.asName(Tx.EN, it) })
    }

    @Test fun `while the stage is unread the button cycles Existing, Client wish, Vendor added, per capture`() {
        var picked: String? = null; val seen = ArrayList<String>(); val said = ArrayList<String>()
        repeat(4) {
            val c = chipAt(null, "W", picked = picked)!!
            assertTrue(c.cycles); seen.add(c.text); said.add(c.asLabel.orEmpty())
            picked = PhaseTools.nextAs(null, picked)
        }
        // driven on the page: four faces of the chip, a tap on its button between each
        assertEquals(listOf("Placing Window · tap the photo · as Existing ▾", "Placing Window · tap the photo · as Client wish ▾",
            "Placing Window · tap the photo · as Vendor added ▾", "Placing Window · tap the photo · as Existing ▾"), seen)
        assertEquals(listOf("Place marks as Existing — tap to change", "Place marks as Client wish — tap to change",
            "Place marks as Vendor added — tap to change", "Place marks as Existing — tap to change"), said)
        chipAt(null, "W", picked = "wish")!!.let { c -> assertEquals("as Client wish ▾", c.text.substring(c.asFrom)); assertEquals("Placing Window · tap the photo · Place marks as Client wish — tap to change", c.spoken) }
        // nothing cycles where the phase fixes it, at Survey, or where there is no palette
        for (st in 0..5) assertNull(PhaseTools.nextAs(st, "wish"), "$st")
    }

    @Test fun `the chip in Marathi, as the page draws it`() {
        assertEquals("इलेक्ट्रिक पॉइंट ठेवत आहे · फोटोवर टॅप करा · ग्राहकाची इच्छा म्हणून", chipAt(1, tx = Tx.MR)!!.text)
        assertEquals("या फेजमध्ये मार्क ग्राहकाची इच्छा म्हणून ठेवले जातात", chipAt(1, tx = Tx.MR)!!.asLabel)
        assertEquals("इलेक्ट्रिक पॉइंट ठेवत आहे · फोटोवर टॅप करा · व्हेंडरने जोडले म्हणून", chipAt(4, tx = Tx.MR)!!.text)
        assertEquals("इलेक्ट्रिक पॉइंट ठेवत आहे · फोटोवर टॅप करा · सध्याचे म्हणून ▾", chipAt(null, tx = Tx.MR)!!.text)
        assertEquals("मार्क सध्याचे म्हणून ठेवा — बदलण्यासाठी टॅप करा", chipAt(null, tx = Tx.MR)!!.asLabel)
        assertEquals("इलेक्ट्रिक पॉइंट ठेवत आहे · फोटोवर टॅप करा · ग्राहकाची इच्छा म्हणून ▾", chipAt(null, picked = "wish", tx = Tx.MR)!!.text)
    }

    // ------------------------------------------------------------------ port 62 slice L6: More ▸ by trade, the kind on the chip, its list, the tool's memory

    private val KINDS6 = listOf("wall", "floor", "ceil")
    private val SURF6 = mapOf("wall" to "front", "floor" to "floor", "ceil" to "ceil")
    private fun hex6(c: Int) = "#" + c.toString(16).padStart(6, '0')

    /** The rows `android/docs/port62/proto-tables.mjs` prints between "L6 GOLDEN" and "L6 END", from the page's own code. */
    private fun golden6(): List<String> = requireNotNull(javaClass.getResourceAsStream("/palette/proto-palette-rows.txt")) { "the golden is missing" }
        .readBytes().decodeToString().lines().filter { it.isNotEmpty() }

    /** The same rows, printed from SurveyPrepPalette. */
    private fun rows6(): List<String> {
        val out = ArrayList<String>()
        for ((lang, tx) in listOf("en" to Tx.EN, "mr" to Tx.MR)) for (k in KINDS6) {
            val rows = SurveyPrepPalette.moreRows(tx, SURF6.getValue(k)); val cols = SurveyPrepPalette.moreColumns(rows, true)
            out += "more $lang $k ${rows.size} rows, landscape ${cols[0].size} + ${cols.getOrNull(1)?.size ?: 0}"
            for (r in rows) out += "more $lang $k ${r.label} ${hex6(r.colour)}: " + r.tiles.joinToString(", ") { "${it.letter} ${SurveyPrepPalette.short(tx, it.name)}" }
        }
        for (k in KINDS6) out += "slot $k: " + SurveyPrepPalette.paletteFor(k).more.joinToString(" ") { ty ->
            "${ty.letter}=${SurveyPrepPalette.moreLabel(Tx.EN, k, ty.letter).replace(' ', '~')}${if (SurveyPrepPalette.moreLong(Tx.EN, k, ty.letter)) "!" else ""}" }
        for (k in KINDS6) for (ty in SurveyPrepPalette.paletteFor(k).all) {
            val c = SurveyPrepPalette.chip(Tx.EN, SURF6.getValue(k), ty.letter, PhaseTools.SURVEY, null)!!
            out += "chip $k ${ty.letter}: " + (if (c.kindFrom < 0) c.text else c.text.substring(0, c.kindFrom) + "[" + c.text.substring(c.kindFrom, c.kindTo) + "]" + c.text.substring(c.kindTo)) +
                (c.kindLabel?.let { " ~ $it" } ?: "")
        }
        for (k in KINDS6) for (ty in SurveyPrepPalette.paletteFor(k).all) SurveyPrepPalette.kindList(Tx.EN, SURF6.getValue(k), ty.letter, null)?.let { l ->
            out += "kinds $k ${ty.letter}: ${l.title} | " + l.options.joinToString(", ") { "${it.id}=${it.label}${if (it.on) "(on)" else ""}" } }
        return out
    }

    @Test fun `L6 - More by trade, the More slot's name, the chip's kind button and the list of kinds are the page's own, row for row`() {
        val want = golden6(); val got = rows6()
        for (i in 0 until minOf(want.size, got.size)) assertEquals(want[i], got[i], "row ${i + 1}")
        assertEquals(want.size, got.size, "as many rows")
        assertEquals(122, got.size)
        // what the rows say, in short: eleven trades have a type on a wall, six on the floor, eight on the ceiling -- General first
        assertEquals(listOf(11, 6, 8), KINDS6.map { SurveyPrepPalette.moreRows(Tx.EN, SURF6.getValue(it)).size })
        for (k in KINDS6) assertEquals(Trades.GENERAL, SurveyPrepPalette.moreRows(Tx.EN, SURF6.getValue(k)).first().trade, k)
        assertEquals(listOf("NOTE", "X"), SurveyPrepPalette.moreRows(Tx.EN, "front").first().tiles.map { it.letter }, "the Note is the first tile")
    }

    @Test fun `L6 - More lists every type of the surface exactly once, as that surface's own type, on its trade's row`() {
        for (s in SurveyPrep.SURFACES) {
            val rows = SurveyPrepPalette.moreRows(Tx.EN, s)
            val tiles = rows.flatMap { it.tiles }
            assertEquals(SurveyPrep.types(s).map { it.letter }.sorted(), tiles.map { it.letter }.sorted(), s)
            // a tile is the surface's own row of TYPES: the floor's S is the Step, in Civil's row; a wall's the Switch plate, in Electrical's
            for (t in tiles) assertSame(SurveyPrep.typeOf(s, t.letter), t, "$s ${t.letter}")
            for (r in rows) { assertTrue(r.tiles.isNotEmpty(), "$s ${r.trade}"); assertEquals(Trades.byId(r.trade)!!.dot, r.colour); assertEquals(Trades.short(Tx.EN, r.trade), r.label)
                for (t in r.tiles) if (t.letter != "DEF") assertEquals(r.trade, Trades.tradeOf(t.letter, s), "$s ${t.letter} sits in its own trade's row") }
            // a Defect's row is Painting's, its first kind's trade (a broken tile is the tiler's only once that kind is picked)
            assertEquals(Trades.PAINTING, rows.single { r -> r.tiles.any { it.letter == "DEF" } }.trade)
            // no row is longer than four tiles: 80 dp of label and four 48 dp tiles fit a 360 dp screen
            assertTrue(rows.all { it.tiles.size <= 4 }, s)
        }
        assertEquals("Step", SurveyPrepPalette.moreRows(Tx.EN, "floor").single { it.trade == Trades.CIVIL }.tiles.single { it.letter == "S" }.name)
        assertEquals("Switch plate", SurveyPrepPalette.moreRows(Tx.EN, "left").single { it.trade == Trades.ELECTRICAL }.tiles.single { it.letter == "S" }.name)
        // portrait: one column; landscape: two, the first the longer -- and nothing to split when there is one row or none
        val wall = SurveyPrepPalette.moreRows(Tx.EN, "front")
        assertEquals(listOf(wall), SurveyPrepPalette.moreColumns(wall, false))
        assertEquals(listOf(wall.take(6), wall.drop(6)), SurveyPrepPalette.moreColumns(wall, true))
        assertEquals(listOf(wall.take(1)), SurveyPrepPalette.moreColumns(wall.take(1), true)); assertEquals(listOf(emptyList()), SurveyPrepPalette.moreColumns(emptyList(), true))
        assertEquals(listOf(2, 1), SurveyPrepPalette.moreColumns(wall.take(3), true).map { it.size })
    }

    @Test fun `L6 - the More slot names an armed More type, and is drawn tight only past nine characters`() {
        // "Opening ▾" is nine with its narrow space and its arrow: the longest drawn at the slot's own spacing
        assertEquals(9, SurveyPrepPalette.moreLabel(Tx.EN, "wall", "O").length); assertEquals(9, SurveyPrepPalette.MORE_LONG)
        assertFalse(SurveyPrepPalette.moreLong(Tx.EN, "wall", "O"))
        assertEquals(listOf("PLB", "LV", "SAN"), SurveyPrepPalette.paletteFor("wall").more.map { it.letter }.filter { SurveyPrepPalette.moreLong(Tx.EN, "wall", it) })
        // with Select, a drawing tool or a type of the palette itself on, the slot says "More ▸" and is never tight
        for (tool in listOf(SurveyPrepPalette.SEL, "T", "K", "W", "E")) assertFalse(SurveyPrepPalette.moreLong(Tx.EN, "wall", tool), tool)
        // under मराठी it is the Marathi name's own length that decides
        assertEquals(SurveyPrepPalette.moreLabel(Tx.MR, "wall", "SAN").length > 9, SurveyPrepPalette.moreLong(Tx.MR, "wall", "SAN"))
    }

    @Test fun `L6 - the chip names the kind a tap places, as a button - where it sits, what is said for it, and beside the second half`() {
        val c = SurveyPrepPalette.chip(Tx.EN, "front", "CRP", PhaseTools.SURVEY, null)!!
        assertEquals("Placing Wardrobe ▾ · tap the photo", c.text); assertEquals("Wardrobe ▾", c.text.substring(c.kindFrom, c.kindTo))
        assertEquals("Kind: Wardrobe — tap to change", c.kindLabel); assertEquals("Placing Kind: Wardrobe — tap to change · tap the photo", c.spoken)
        assertNull(c.asText); assertEquals(-1, c.asFrom)
        // a type with no kinds on the surface names the type, as v52: no button
        for ((s, t) in listOf("front" to "W", "front" to "X", "front" to "NOTE", "floor" to "S", "ceil" to "F", "floor" to "C")) {
            val plain = SurveyPrepPalette.chip(Tx.EN, s, t, PhaseTools.SURVEY, null)!!
            assertEquals(listOf<Any?>(-1, -1, null), listOf(plain.kindFrom, plain.kindTo, plain.kindLabel), "$s $t")
            assertEquals(SurveyPrepPalette.chipText(Tx.EN, s, t), plain.text, "$s $t"); assertEquals(plain.text, plain.spoken)
        }
        // the wall's S has kinds -- a Switch plate, a DB / MCB board, a Meter board; the floor's S is a Step and has none
        assertEquals("Placing Switch plate ▾ · tap the photo", SurveyPrepPalette.chip(Tx.EN, "left", "S", 0, null)!!.text)
        assertEquals("Placing DB / MCB board ▾ · tap the photo", SurveyPrepPalette.chip(Tx.EN, "left", "S", 0, null, sub = "db")!!.text)
        // the kind remembered is the one named; one this kind of surface has not, or none, reads as the first
        assertEquals("Placing Kitchen base ▾ · tap the photo", SurveyPrepPalette.chip(Tx.EN, "front", "CRP", 0, null, sub = "kbase")!!.text)
        for (sub in listOf(null, "", "island", "zzz")) assertEquals("Placing Wardrobe ▾ · tap the photo", SurveyPrepPalette.chip(Tx.EN, "front", "CRP", 0, null, sub = sub)!!.text, "$sub")
        assertEquals("Placing Island ▾ · tap the photo", SurveyPrepPalette.chip(Tx.EN, "floor", "CRP", 0, null, sub = "island")!!.text)
        assertEquals("Placing Full false ceiling ▾ · tap the photo", SurveyPrepPalette.chip(Tx.EN, "ceil", "CDP", 0, null, sub = "full")!!.text)
        // a Plumbing point keeps its type's name first, and the WHOLE of it is the button
        val p = SurveyPrepPalette.chip(Tx.EN, "front", "PLB", 0, null, sub = "mixer")!!
        assertEquals("Placing Plumbing point · Mixer (hot + cold) ▾ · tap the photo", p.text); assertEquals("Plumbing point · Mixer (hot + cold) ▾", p.text.substring(p.kindFrom, p.kindTo))
        assertEquals("Kind: Plumbing point · Mixer (hot + cold) — tap to change", p.kindLabel)
        // with the phase's second half: the kind button first, the second half after it, each with its own label
        val w = SurveyPrepPalette.chip(Tx.EN, "front", "CRP", PhaseTools.WISH_LIST, null)!!
        assertEquals("Placing Wardrobe ▾ · tap the photo · as Client wish", w.text); assertEquals("Wardrobe ▾", w.text.substring(w.kindFrom, w.kindTo)); assertEquals("as Client wish", w.text.substring(w.asFrom))
        assertTrue(w.kindTo <= w.asFrom); assertFalse(w.cycles)
        assertEquals("Placing Kind: Wardrobe — tap to change · tap the photo · Marks are placed as Client wish in this phase", w.spoken)
        val u = SurveyPrepPalette.chip(Tx.EN, "front", "CRP", null, "vadd", sub = "tv")!!
        assertEquals("Placing TV unit ▾ · tap the photo · as Vendor added ▾", u.text); assertTrue(u.cycles); assertEquals("TV unit ▾", u.text.substring(u.kindFrom, u.kindTo))
        // on the 360, as the page words it
        assertEquals("Placing Wardrobe ▾ · tap the 360", SurveyPrepPalette.chip(Tx.EN, "front", "CRP", 0, null, "360")!!.text)
        assertEquals("Placing Window · tap the 360", SurveyPrepPalette.chip(Tx.EN, "front", "W", 0, null, "360")!!.text)
        // no chip where there is no type on, whatever kind is handed in
        for (t in listOf(SurveyPrepPalette.SEL, "T", "M", "K")) assertNull(SurveyPrepPalette.chip(Tx.EN, "front", t, 0, null, sub = "kbase"), t)
    }

    @Test fun `L6 - the chip and the list in Marathi - the button is found wherever the sentence puts it`() {
        // मराठी puts the name FIRST: the button is found by a marker, not by its English place (the page: "वॉर्डरोब ▾ ठेवत आहे · फोटोवर टॅप करा")
        val c = SurveyPrepPalette.chip(Tx.MR, "front", "CRP", PhaseTools.WISH_LIST, null)!!
        assertEquals("वॉर्डरोब ▾ ठेवत आहे · फोटोवर टॅप करा · ग्राहकाची इच्छा म्हणून", c.text)
        assertEquals(listOf<Any?>(0, "वॉर्डरोब ▾"), listOf(c.kindFrom, c.text.substring(c.kindFrom, c.kindTo)))
        assertEquals("प्रकार: वॉर्डरोब — बदलण्यासाठी टॅप करा", c.kindLabel)
        assertEquals("प्रकार: वॉर्डरोब — बदलण्यासाठी टॅप करा ठेवत आहे · फोटोवर टॅप करा · या फेजमध्ये मार्क ग्राहकाची इच्छा म्हणून ठेवले जातात", c.spoken)
        assertEquals("प्लंबिंग पॉइंट · थंड ▾ ठेवत आहे · फोटोवर टॅप करा", SurveyPrepPalette.chip(Tx.MR, "front", "PLB", 0, null)!!.text)
        assertEquals("वॉर्डरोब ▾ ठेवत आहे · 360 वर टॅप करा", SurveyPrepPalette.chip(Tx.MR, "front", "CRP", 0, null, "360")!!.text)
        assertEquals("नोंद ठेवत आहे · फोटोवर टॅप करा", SurveyPrepPalette.chip(Tx.MR, "front", "NOTE", 1, null)!!.text)
        // the list: the type's name, "· प्रकार", and each kind by its own label
        val l = SurveyPrepPalette.kindList(Tx.MR, "front", "CRP", "kbase")!!
        assertEquals("सुतारकाम युनिट · प्रकार", l.title)
        assertEquals(listOf("वॉर्डरोब", "किचन खालचे युनिट(on)", "किचन वरचे युनिट", "TV युनिट", "स्टडी", "शू रॅक", "व्हॅनिटी", "इतर युनिट"), l.options.map { it.label + if (it.on) "(on)" else "" })
        // A Plumbing point's list is titled by the KEY (sp.x_kind): the page's text layer, which finds a text by its English,
        // matches "Plumbing point · kind" as a Plumbing point of the kind "kind" and shows "प्लंबिंग पॉइंट · kind"
        assertEquals("प्लंबिंग पॉइंट · प्रकार", SurveyPrepPalette.kindList(Tx.MR, "floor", "PLB", null)!!.title)
        assertEquals(listOf("फ्लोअर ट्रॅप(on)", "वेस्ट आउटलेट", "थंड", "गरम"), SurveyPrepPalette.kindList(Tx.MR, "floor", "PLB", null)!!.options.map { it.label + if (it.on) "(on)" else "" })
        // More's rows: the trades' short names
        assertEquals(listOf("सामान्य", "सिव्हिल", "दारे-खिडक्या", "ग्रिल", "इलेक्ट्रिकल", "CCTV व डेटा", "AC", "सुतारकाम", "प्लंबिंग", "रंगकाम", "पडदे"), SurveyPrepPalette.moreRows(Tx.MR, "front").map { it.label })
    }

    @Test fun `L6 - the list of kinds - none for a type with no kinds, the kind remembered ringed, in the type's colour`() {
        for ((s, t) in listOf("front" to "W", "floor" to "S", "front" to "NOTE", "front" to "X", "front" to SurveyPrepPalette.SEL, "front" to "T", "front" to "K", "floor" to "GRL", "front" to "CDP"))
            assertNull(SurveyPrepPalette.kindList(Tx.EN, s, t, "kbase"), "$s $t")
        val l = SurveyPrepPalette.kindList(Tx.EN, "front", "CRP", "kbase")!!
        assertEquals("Carpentry unit · kind", l.title); assertEquals(0xa0522d, l.colour); assertEquals(SurveyPrepPalette.chipColour("front", "CRP"), l.colour)
        assertEquals(listOf("wardrobe", "kbase", "kwall", "tv", "study", "shoe", "vanity", "other"), l.options.map { it.id }); assertEquals(listOf("kbase"), l.options.filter { it.on }.map { it.id })
        // an unknown or absent kind rings the first; exactly one is ever ringed; eight kinds at most (four rows of two)
        for (sub in listOf(null, "", "zzz", "bed")) assertEquals(listOf("wardrobe"), SurveyPrepPalette.kindList(Tx.EN, "front", "CRP", sub)!!.options.filter { it.on }.map { it.id }, "$sub")
        for (s in SurveyPrep.SURFACES) for (ty in SurveyPrep.types(s)) for (sub in listOf(null) + MarkTypes.subsOf(ty.letter, s).map { it.id }) SurveyPrepPalette.kindList(Tx.EN, s, ty.letter, sub)?.let { k ->
            assertEquals(1, k.options.count { it.on }, "$s ${ty.letter} $sub"); assertTrue(k.options.size in 2..8, "$s ${ty.letter}")
            assertEquals(MarkTypes.subsOf(ty.letter, s).map { it.id }, k.options.map { it.id })
            // the chip has a kind button exactly where there is a list
            assertTrue(SurveyPrepPalette.chip(Tx.EN, s, ty.letter, 0, null, sub = sub)!!.kindFrom >= 0, "$s ${ty.letter}") }
        for (s in SurveyPrep.SURFACES) for (ty in SurveyPrep.types(s)) if (SurveyPrepPalette.kindList(Tx.EN, s, ty.letter, null) == null)
            assertEquals(-1, SurveyPrepPalette.chip(Tx.EN, s, ty.letter, 0, null)!!.kindFrom, "$s ${ty.letter}")
        // the wall's S: titled by the type, whatever kind is on
        assertEquals("Switch plate · kind", SurveyPrepPalette.kindList(Tx.EN, "back", "S", "meter")!!.title)
    }

    @Test fun `L6 - the tool remembers the last kind, per type AND kind of surface - the page's own journey`() {
        val mem = HashMap<String, String>()
        fun show() = listOf("CRP", "PLB", "LV", "S").flatMap { t -> KINDS6.filter { MarkTypes.subsOf(t, it).isNotEmpty() }.map { k -> "$t@$k=${SurveyPrepPalette.subFor(mem, t, SURF6.getValue(k))}" } }.joinToString(" ")
        // proto-tables.mjs, "L6 the tool remembers the last kind": each line is what the page's subFor answers after the same setSub
        assertEquals("CRP@wall=wardrobe CRP@floor=bed PLB@wall=cold PLB@floor=trap LV@wall=camera LV@ceil=camera S@wall=switch", show(), "untouched")
        SurveyPrepPalette.rememberSub(mem, "CRP", "front", "kbase")
        assertEquals("CRP@wall=kbase CRP@floor=bed PLB@wall=cold PLB@floor=trap LV@wall=camera LV@ceil=camera S@wall=switch", show())
        assertEquals("Placing Kitchen base ▾ · tap the photo", SurveyPrepPalette.chip(Tx.EN, "front", "CRP", 0, null, sub = SurveyPrepPalette.subFor(mem, "CRP", "front"))!!.text)
        assertEquals("Placing Bed ▾ · tap the photo", SurveyPrepPalette.chip(Tx.EN, "floor", "CRP", 0, null, sub = SurveyPrepPalette.subFor(mem, "CRP", "floor"))!!.text, "the floor's tool is another tool")
        SurveyPrepPalette.rememberSub(mem, "PLB", "floor", "cold"); SurveyPrepPalette.rememberSub(mem, "PLB", "left", "waste")
        assertEquals("CRP@wall=kbase CRP@floor=bed PLB@wall=waste PLB@floor=cold LV@wall=camera LV@ceil=camera S@wall=switch", show())
        // a kind that kind of surface has not, a kind nobody has, none, and a type with no kinds: nothing is remembered
        val was = HashMap(mem)
        SurveyPrepPalette.rememberSub(mem, "CRP", "front", "island"); SurveyPrepPalette.rememberSub(mem, "CRP", "front", "zzz"); SurveyPrepPalette.rememberSub(mem, "CRP", "front", null)
        SurveyPrepPalette.rememberSub(mem, "W", "front", "x"); SurveyPrepPalette.rememberSub(mem, "S", "floor", "db")
        assertEquals(was, mem)
        SurveyPrepPalette.rememberSub(mem, "S", "back", "db")
        assertEquals("CRP@wall=kbase CRP@floor=bed PLB@wall=waste PLB@floor=cold LV@wall=camera LV@ceil=camera S@wall=db", show())
        // every wall is the one tool: the memory is by KIND of surface, never by the face
        assertEquals(listOf("kbase", "kbase", "kbase", "kbase"), listOf("front", "right", "back", "left").map { SurveyPrepPalette.subFor(mem, "CRP", it) })
        assertEquals("CRP@wall", SurveyPrepPalette.subKey("CRP", "left")); assertEquals("PLB@floor", SurveyPrepPalette.subKey("PLB", "floor")); assertEquals("LV@ceil", SurveyPrepPalette.subKey("LV", "ceil"))
        // a type with no kinds there has none to remember; a memory that holds a kind the type no longer has reads the first
        assertNull(SurveyPrepPalette.subFor(mem, "W", "front")); assertNull(SurveyPrepPalette.subFor(mem, "S", "floor")); assertNull(SurveyPrepPalette.subFor(mem, "CDP", "front"))
        assertEquals("wardrobe", SurveyPrepPalette.subFor(mapOf("CRP@wall" to "gone"), "CRP", "front"))
        // WHAT A TAP STORES is the memory of the surface it LANDS on (the L5 review, finding 2): picked on a wall, tapped onto the floor
        val p = SurveyPrep.fresh(3319.0, 2678.0, 2677.0); val at = java.time.Instant.parse("2026-11-11T05:00:00Z")
        fun tap(s: String, t: String, id: String) = assertNotNull(PhaseTools.place(p, s, t, 500.0, 500.0, 2678.0, 2677.0, id, PhaseTools.SURVEY, null, at, SurveyPrepPalette.subFor(mem, t, s)) { }).mark
        assertEquals(listOf("Plumbing point · Waste outlet", "waste"), tap("front", "PLB", "a").let { listOf(MarkTypes.displayName(Tx.EN, "front", it), it.extra["sub"]) })
        assertEquals(listOf("Plumbing point · Cold", "cold"), tap("floor", "PLB", "b").let { listOf(MarkTypes.displayName(Tx.EN, "floor", it), it.extra["sub"]) })
        assertEquals(listOf<Any?>("Kitchen base", "kbase"), tap("left", "CRP", "c").let { listOf(MarkTypes.displayName(Tx.EN, "left", it), it.extra["sub"]) })
        assertEquals(listOf<Any?>("Bed", null), tap("floor", "CRP", "d").let { listOf(MarkTypes.displayName(Tx.EN, "floor", it), it.extra["sub"]) }, "the floor's first kind: nothing stored")
        assertEquals(listOf<Any?>("Window", null), tap("front", "W", "e").let { listOf(MarkTypes.displayName(Tx.EN, "front", it), it.extra["sub"]) })
        // ... and a kind's own size goes with it: a Kitchen base is 1800 x 850
        p.markById("c")!!.second.let { assertEquals(listOf(1800.0, 850.0), listOf(it.x1 - it.x0, it.y1 - it.y0)) }
    }

    // ------------------------------------------------------------------ the wiring, read from the app's sources (compiled only in CI)

    private val appDir6 = java.io.File("../app/src/main/kotlin/com/malletcrafts/interiorestimate")
    private fun app6(name: String): String = java.io.File(appDir6, name).readLines()
        .filter { l -> l.trimStart().let { !it.startsWith("//") && !it.startsWith("*") && !it.startsWith("/*") } }.joinToString("\n")
    private fun count6(src: String, what: String) = src.windowed(what.length) { if (it == what) 1 else 0 }.sum()

    @Test fun `L6 - the app places the kind remembered for the surface the mark LANDS on, and the chip, the list and More read the one memory`() {
        val screen = app6("SurveyPrepScreen.kt"); val p3 = app6("SurveyPrep360.kt"); val tools = app6("SurveyPrepTools.kt")
        // ONE memory, per app run, read and written through SurveyPrepPalette alone
        assertEquals(1, count6(tools, "internal val spKinds = mutableStateMapOf<String, String>()"))
        val files = appDir6.listFiles { x -> x.name.endsWith(".kt") }!!.map { it.name }.sorted()
        for (f in files) for (m in Regex("""spKinds\b(?!\s*=\s*mutableStateMapOf)""").findAll(app6(f))) {
            val before = app6(f).substring(maxOf(0, m.range.first - 32), m.range.first)
            assertTrue(before.endsWith("SurveyPrepPalette.subFor(") || before.endsWith("SurveyPrepPalette.rememberSub("), "$f: spKinds is read or written past SurveyPrepPalette: …$before")
        }
        // both placements hand PhaseTools.place the kind of the surface the mark is placed ON: `s` -- on the 360 the surface
        // under the finger, a parameter of place360; never the open tab
        assertEquals(2, count6(screen, "quoteStage, asPicked, java.time.Instant.now(), SurveyPrepPalette.subFor(spKinds, t, s)) { snapshot() } ?: return null"))
        assertTrue(screen.contains("fun place360(s: String, t: String, mx: Double, my: Double): SurveyPrep.Mark? {"), "the 360's placement is given the landed surface as s")
        assertTrue(screen.substringAfter("fun placeMark(t: String, mx: Double, my: Double): SurveyPrep.Mark? {").substringBefore("fun place360(").contains("val s = tab; val a = ab(s)"), "2 · Mark places on the open face")
        assertEquals(0, count6(screen, "subFor(spKinds, t, tab)"), "a placement never asks the open tab's memory")
        // the chip names the open face's kind, and its list is that kind's -- on 2 · Mark and on the 360
        assertEquals(1, count6(screen, "val kindNow = SurveyPrepPalette.subFor(spKinds, tool, tab)")); assertEquals(1, count6(p3, "val kindNow = SurveyPrepPalette.subFor(spKinds, tool, tab)"))
        assertEquals(1, count6(screen, "SurveyPrepPalette.chip(tx, tab, tool, stage, asPicked, sub = kindNow)")); assertEquals(1, count6(p3, "SurveyPrepPalette.chip(tx, tab, tool, stage, asPicked, \"360\", kindNow)"))
        for (src in listOf(screen, p3)) {
            assertEquals(1, count6(src, "val kinds = if (typeChip == null || typeChip.kindFrom < 0) null else SurveyPrepPalette.kindList(tx, tab, tool, kindNow)"))
            assertEquals(1, count6(src, "kindFrom = typeChip?.kindFrom ?: -1, kindTo = typeChip?.kindTo ?: -1, kindLabel = typeChip?.kindLabel,"))
            // the button opens and shuts the list; a pick is remembered and shuts it; a press on the photo shuts it
            assertEquals(1, count6(src, "onKind = { kindOpen.value = !kindOpen.value }"))
            assertEquals(1, count6(src, "if (kinds != null && kindOpen.value) {"))
            assertEquals(1, count6(src, "SurveyPrepPalette.rememberSub(spKinds, tool, tab, id); kindOpen.value = false }"))
            assertTrue(Regex("""val first = awaitFirstDown\(\)\n\s+first\.consume\(\)\n\s+kindOpenS\.value = false\n""").containsMatchIn(src), "any press on the photo shuts the list of kinds")
        }
        // More ▸ is drawn from the rows and columns, and the screen hands it the orientation; 700 dp at the right in landscape
        assertTrue(tools.contains("val columns = SurveyPrepPalette.moreColumns(SurveyPrepPalette.moreRows(tx, s), land)"))
        assertTrue(screen.contains("SpMoreSheet(tab, tool, land,\n                    if (land) Modifier.align(Alignment.BottomEnd).widthIn(max = 700.dp).fillMaxWidth() else Modifier.align(Alignment.BottomCenter).fillMaxWidth(),"))
        assertFalse(tools.contains("chunked(5)"), "v52's grid of five is gone")
        assertTrue(tools.contains("val tight = slot.kind == SurveyPrepPalette.SlotKind.MORE && SurveyPrepPalette.moreLong(tx, kind, tool)"))
        // each of the ten new types has an icon by its name, as the twelve have
        for (n in MarkTypes.NEW) assertEquals(1, count6(tools, "        \"${n.name}\" -> {"), "the icon of ${n.name}")
        // the two new points have their own symbol in a spot, from ONE painter on the squared face and on the 360
        val draw = app6("SurveyPrepDraw.kt")
        assertEquals(1, count6(draw, "fun newSpotSym(c: Canvas, x: Float, y: Float, k: Float, t: String, fill: Paint, line: Paint): Boolean {"))
        assertEquals(1, count6(draw, "\"PLB\", \"LV\" -> {\n                p.style = Paint.Style.FILL\n                newSpotSym(c, x, y, k, t, p,")); assertEquals(1, count6(p3, "\"PLB\", \"LV\" -> SurveyPrepDraw.newSpotSym(c, x, y, k, t,"))
    }

    /**
     * THE L6 REVIEW (the list of kinds came back open by itself). The first version kept the face and tool the list was
     * opened FOR (`kindFor`, a key "front|CRP") in a state that outlived both, and drew the list while it matched: ✕ on
     * the chip, then the same type again -- or another face and back -- matched again, and the list was open unasked. The
     * page shuts it on every tool change (setTool: st.subpick=false, index61c2.html 6641) and on a click outside the
     * list and its button (6811). The state is the screen's and :app is compiled only in CI, so this reads the wiring:
     * the open state is a NEW one for each face, step and tool; nothing keeps a key that can match again; and inside
     * the photo's box fit and grid shut it as the photo does. DRIVEN on the page (DESIGN-port62, "The L6 review"):
     * More ▸ and the chip's cycling "as …" button keep their click to themselves (6618, 3091) and leave the list OPEN
     * -- so neither shuts it here, and More ▸ open is no key of the state.
     */
    @Test fun `L6 review - the chip's list of kinds dies with the face, step and tool it was opened on - fit and grid shut it, More and the cycling as-button do not`() {
        val screen = app6("SurveyPrepScreen.kt"); val p3 = app6("SurveyPrep360.kt")
        // a new state for each: 2 · Mark's by its face, step and tool; the 360's by its tab and tool
        assertEquals(1, count6(screen, "val kindOpen = remember(tab, mode, tool) { mutableStateOf(false) }"))
        assertEquals(1, count6(p3, "val kindOpen = remember(tab, tool) { mutableStateOf(false) }"))
        for (src in listOf(screen, p3)) {
            // nothing is left that remembers WHAT the list was opened for, to match again later
            assertFalse(src.contains("kindFor"), "the key that outlived its tool is gone"); assertFalse(src.contains("kindKey"))
            assertEquals(1, Regex("""\bkindOpen\b\s*(=|by)\s*remember""").findAll(src).count(), "one open state, and it is the keyed one")
            // the gesture outlives recompositions (its pointerInput is not keyed on the tool): it shuts the CURRENT state
            assertEquals(1, count6(src, "val kindOpenS by rememberUpdatedState(kindOpen)"))
            assertEquals(1, count6(src, "kindOpenS.value = false"), "the photo's press, through the State")
            // every write there is: a pick, fit and grid shut it; the kind button alone opens it
            assertEquals(3, count6(src, "kindOpen.value = false"))
            assertEquals(1, count6(src, "kindOpen.value = !kindOpen.value"))
            assertEquals(4, Regex("""\bkindOpen\.value\s*=[^=]""").findAll(src).count(), "no other write")
            // the cycling "as …" button goes straight to the screen: on the page it leaves the list open
            assertEquals(1, count6(src, "onAs = onAs,"))
        }
        assertEquals(1, count6(screen, "kindOpen.value = false; view = view.fit(); tick++ }")); assertEquals(1, count6(screen, ".clickable(role = Role.Button) { kindOpen.value = false; onGrid() }"))
        assertEquals(1, count6(p3, "kindOpen.value = false; act.look(Pano360.faceView(tab, 1.0, 1.0)) }")); assertEquals(1, count6(p3, ".clickable(role = Role.Button) { kindOpen.value = false; act.grid() }"))
        // More ▸ only opens and shuts its sheet: it touches neither the tool nor the list
        assertEquals(1, count6(screen, "SpPalette(tab, tool, land, onTool = { t -> pickTool(t) }, onMore = { moreOpen = !moreOpen })"))
        // what DOES end it from outside the photo: a face or a step pressed -- the open one too -- goes back to Select, and
        // a palette slot or a More tile changes the tool or lets it go (SurveyPrepPalette.next); each is a new state
        assertTrue(screen.contains("tab = t; sel = null; menuOpen = false; toolSet = SurveyPrepPalette.SEL; moreOpen = false;"), "showTab: back to Select")
        assertTrue(screen.contains("modeSet = m; sel = null; toolSet = SurveyPrepPalette.SEL; moreOpen = false;"), "setMode: back to Select")
        assertTrue(screen.contains("ref = refT, land = land) { showTab(t) }"), "a tab pressed is showTab, the open tab too")
        assertEquals(SurveyPrepPalette.SEL, SurveyPrepPalette.next("CRP", "CRP"), "the same type picked again lets the tool go")
        assertEquals("CRP", SurveyPrepPalette.next("W", "CRP"))
    }
}
