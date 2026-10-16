package com.malletcrafts.sitephotos.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
            SurveyPrepPalette.paletteFor("wall").all.joinToString("|") { SurveyPrepPalette.short(it.name) })
        assertEquals("Column|Step|Opening|Electric|Bulb|Other",
            SurveyPrepPalette.paletteFor("floor").all.joinToString("|") { SurveyPrepPalette.short(it.name) })
        assertEquals("Every mark type · Front wall", SurveyPrepPalette.sheetTitle("front"))
        assertEquals("Every mark type · Ceiling", SurveyPrepPalette.sheetTitle("ceil"))
    }

    @Test fun `slots now Select, three types, More, and the tools slot in before the types`() {
        val now = SurveyPrepPalette.slots("wall")
        assertEquals(5, now.size)
        assertEquals("Select|Window|Door|Electric|More ▸", now.joinToString("|") { s ->
            when (s.kind) {
                SurveyPrepPalette.SlotKind.SELECT -> "Select"
                SurveyPrepPalette.SlotKind.MORE -> SurveyPrepPalette.moreLabel("wall", SurveyPrepPalette.SEL)
                else -> SurveyPrepPalette.short(s.type?.name ?: s.key)
            } })
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
        assertEquals("Loft ▾", SurveyPrepPalette.moreLabel("wall", "L"))
        // FX8: every More type names itself in full, "Opening ▾" too
        assertEquals("Opening ▾", SurveyPrepPalette.moreLabel("wall", "O"))
        assertEquals("Switch ▾", SurveyPrepPalette.moreLabel("wall", "S"))
        assertEquals("Bulb ▾", SurveyPrepPalette.moreLabel("floor", "P"))
        assertEquals("Column ▾", SurveyPrepPalette.moreLabel("ceil", "C"))
        // a main type, or Select, leaves it "More ▸"
        assertEquals("More ▸", SurveyPrepPalette.moreLabel("wall", "W"))
        assertEquals("More ▸", SurveyPrepPalette.moreLabel("wall", SurveyPrepPalette.SEL))
        // the floor's S is a Step, on the palette itself: More is not lit
        assertNull(SurveyPrepPalette.moreArmed("floor", "S"))
    }

    @Test fun `exactly one slot is lit`() {
        for ((kind, tool) in listOf("wall" to SurveyPrepPalette.SEL, "wall" to "D", "wall" to "L", "ceil" to "F", "floor" to "S")) {
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
        assertNull(SurveyPrepPalette.chipText("front", SurveyPrepPalette.SEL))
        assertEquals("Placing Door · tap the photo", SurveyPrepPalette.chipText("front", "D"))
        assertEquals("Placing Switch plate · tap the photo", SurveyPrepPalette.chipText("left", "S"))
        assertEquals("Placing Step · tap the photo", SurveyPrepPalette.chipText("floor", "S"))
    }
}
