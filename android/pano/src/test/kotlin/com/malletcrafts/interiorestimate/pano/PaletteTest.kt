package com.malletcrafts.interiorestimate.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * The 2 · Mark palette (v52 slice 7), against ct52 section T ('palette types', 'Loft is in More ▸ now') and
 * FX8 ('Opening ▾' with U+202F). The '8 slots' check completes in slice 11, when Chain joins: here the slot
 * list is Select, the three types and More ▸, and the drawing tools slot in between when they exist.
 */
class PaletteTest {

    private fun f(kind: String) = SurveyPrepPalette.paletteFor(kind).let { p ->
        p.main.joinToString("") { it.letter } + "/" + p.more.joinToString("") { it.letter } }

    @Test fun `palette types per kind, More in TYPES order`() {
        // ct52 T: walls W D E (More: C B O N L P S F X), floor C S E (More: O P X), ceiling B E P (More: C F O X)
        assertEquals("WDE/CBONLPSFX", f("wall"))
        assertEquals("CSE/OPX", f("floor"))
        assertEquals("BEP/CFOX", f("ceil"))
        assertEquals("WDE/CBONLPSFX", SurveyPrepPalette.forSurface("left").let { p ->
            p.main.joinToString("") { it.letter } + "/" + p.more.joinToString("") { it.letter } })
    }

    @Test fun `the More sheet lists every type with its short name`() {
        // ct52 T: the sheet reads Window|Door|Column|Beam|Opening|Niche|Loft|Electric|Bulb|Switch|Fan|Other
        assertEquals("Window|Door|Column|Beam|Opening|Niche|Loft|Electric|Bulb|Switch|Fan|Other",
            SurveyPrepPalette.paletteFor("wall").all.joinToString("|") { SurveyPrepPalette.short(Tx.EN, it.name) })
        assertEquals("Column|Step|Opening|Electric|Bulb|Other",
            SurveyPrepPalette.paletteFor("floor").all.joinToString("|") { SurveyPrepPalette.short(Tx.EN, it.name) })
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
}
