package com.malletcrafts.interiorestimate.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * THE MEASURE BAR'S TEXTS, IN ENGLISH (port 63 slice M1; DESIGN-port63 §1.4, §1.5 and §3). English first (Amit, 16 Nov
 * 2026): the new `sp.measure.*` keys are pinned in English only -- their Marathi waits in android/i18n/mr-pending.tsv,
 * and MrPendingTest holds it to that. What the bar reads from keys the app already had keeps the Marathi it had.
 */
class MeasureTextTest {

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0
    private fun ab(s: String): DoubleArray = SurveyPrep.size(s, L, W, H)
    private fun pt(x: Double, y: Double) = doubleArrayOf(x, y)

    private fun SurveyPrep.Prep.put(s: String, t: String, n: Int, x0: Double, x1: Double, y0: Double, y1: Double, sub: String? = null,
                                    extra: Map<String, Any?> = emptyMap()): SurveyPrep.Mark =
        SurveyPrep.Mark("id-$s-$t$n", t, SurveyPrep.CODE.getValue(s) + t + n, x0, x1, y0, y1, MarkTypes.withSub(extra, t, s, sub))
            .also { surface(s).marks.add(it) }

    private fun room(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(L, W, H)
        p.put("front", "D", 1, 300.0, 1200.0, 0.0, 2100.0)
        p.put("front", "W", 1, 1500.0, 2400.0, 900.0, 2100.0)
        p.put("front", "E", 1, 1325.0, 1375.0, 275.0, 325.0)
        p.put("front", "S", 1, 150.0, 350.0, 1100.0, 1190.0, extra = mapOf(SurveyPrep.LOCKED to true))
        p.put("front", "S", 2, 150.0, 450.0, 1500.0, 1900.0, sub = "db")
        p.put("front", "CRP", 1, 150.0, 1350.0, 0.0, 2100.0, sub = "kbase")
        p.put("front", "SAN", 1, 1000.0, 1550.0, 400.0, 850.0)
        p.put("front", MarkTypes.NOTE, 1, 100.0, 150.0, 1200.0, 1250.0)
        p.put("floor", "S", 1, 800.0, 1700.0, 150.0, 450.0)
        p.addMeas("front", SurveyMeas.TAPE, listOf(pt(0.0, 1000.0), pt(1234.0, 1000.0)), "t1")
        p.addMeas("front", SurveyMeas.LINE, listOf(pt(500.0, 1600.0), pt(1300.0, 2200.0)), "m1")
        p.addMeas("front", SurveyMeas.AREA, listOf(pt(1800.0, 300.0), pt(2400.0, 300.0), pt(2400.0, 800.0)), "m2")
        p.addChain("front", "x", listOf(pt(300.0, 2400.0), pt(1200.0, 2400.0), pt(2400.0, 2400.0)), listOf(null, null, null), "k1")
        return p
    }

    @Test fun `line 1 is the measure's own words alone -- no code, no type`() {
        val p = room()
        fun label(code: String) = MeasureText.label(Tx.EN, p, code, ::ab)
        assertEquals("width", label("FD1.w"))
        assertEquals("from left corner", label("FD1.l"))
        assertEquals("reveal depth", label("FD1.p"))
        assertEquals("sill, from floor", label("FW1.s"))
        assertEquals("from right corner, to centre", label("FE1.cr"))       // the longest: 28 characters, cut on the narrowest bar
        assertEquals("underside, from floor", label("FSAN1.s"))
        assertEquals("length", label("F-M1.len"))
        assertEquals("perimeter", label("F-M2.per"))
        assertEquals("segment 2", label("FK1.2"))
        assertEquals("total", label("FK1.t"))
        // a key nothing asks for: a mark that is gone, a tape, a measure the mark's geometry drops (the door touches the floor)
        assertNull(label("FZ9.w")); assertNull(label("F-T1.len")); assertNull(label("FD1.s"))
        // the measure names are keys the app already had: under मराठी they read as they always did
        assertEquals("डाव्या कोपऱ्यापासून", MeasureText.label(Tx.MR, p, "FD1.l", ::ab))
        assertEquals("परिमिती", MeasureText.label(Tx.MR, p, "F-M2.per", ::ab))
    }

    @Test fun `the label cell is the code over the type's short name, or over locked`() {
        val p = room()
        fun cell(code: String) = MeasureText.cell(Tx.EN, p, p.markOf("$code.x")!!.second.id)?.let { it.code to it.under }
        assertEquals("FD1" to "Door", cell("FD1"))
        assertEquals("FW1" to "Window", cell("FW1"))
        assertEquals("FE1" to "Electric", cell("FE1"))                        // the SHORT name: "Electric point" does not fit 52 dp
        assertEquals("FS2" to "Switch", cell("FS2"))                          // the type's short name, whatever its kind
        assertEquals("FCRP1" to "Unit", cell("FCRP1"))
        assertEquals("FSAN1" to "Sanitary", cell("FSAN1"))
        assertEquals("FNOTE1" to "Note", cell("FNOTE1"))
        assertEquals("GS1" to "Step", cell("GS1"))                            // the floor's S is a Step
        assertEquals("FS1" to "locked", cell("FS1"))                          // a locked mark says so under its code
        assertEquals("FD1\nDoor", MeasureText.cell(Tx.EN, p, "id-front-D1")!!.text)
        // a Line's and an area's code alone; a tape and a chain have no label (they open no Mark sheet), nor has an id nothing has
        assertEquals("F-M1" to null, MeasureText.cell(Tx.EN, p, "m1")?.let { it.code to it.under })
        assertEquals("F-M2", MeasureText.cell(Tx.EN, p, "m2")!!.text)
        assertNull(MeasureText.cell(Tx.EN, p, "t1")); assertNull(MeasureText.cell(Tx.EN, p, "k1")); assertNull(MeasureText.cell(Tx.EN, p, "nothing"))
        // keys the app already had: under मराठी the cell reads as the palette and the row do
        assertEquals("FS1\nलॉक", MeasureText.cell(Tx.MR, p, "id-front-S1")!!.text)
        assertEquals("FD1\nदरवाजा", MeasureText.cell(Tx.MR, p, "id-front-D1")!!.text)
    }

    @Test fun `progress, a finished face, the next face and a finished room`() {
        assertEquals("7 of 18 measured", MeasureText.progress(Tx.EN, MeasureWalk.Count(7, 18)))
        assertEquals("0 of 4 measured", MeasureText.progress(Tx.EN, MeasureWalk.Count(0, 4)))
        assertEquals("Front wall · all 9 measured ✓", MeasureText.faceDone(Tx.EN, "front", 9))
        assertEquals("Ceiling · all 2 measured ✓", MeasureText.faceDone(Tx.EN, "ceil", 2))
        assertEquals("Next: Right wall · 4 to measure ▸", MeasureText.nextFace(Tx.EN, MeasureWalk.Face("right", 4)))
        assertEquals("Next: Floor · 1 to measure ▸", MeasureText.nextFace(Tx.EN, MeasureWalk.Face("floor", 1)))
        assertEquals("Room measured · all 31 ✓", MeasureText.roomDone(Tx.EN, 31))
        // from the walk itself: a door's four on the front wall, two read
        val p = SurveyPrep.fresh(L, W, H)
        p.put("front", "D", 1, 300.0, 1200.0, 0.0, 2100.0)
        p.actual["FD1.w"] = 900.0; p.actual["FD1.h"] = 2100.0
        assertEquals("2 of 4 measured", MeasureText.progress(Tx.EN, MeasureWalk.count(p, "front", W, H)))
    }

    @Test fun `the message line's two sentences`() {
        assertEquals("The photo says ≈640 mm. Check the mark, or shoot again.", MeasureText.photoSays(Tx.EN, 640.0))
        // rounded as the figure on the plate is
        assertEquals("The photo says ≈1203 mm. Check the mark, or shoot again.", MeasureText.photoSays(Tx.EN, 1203.4))
        assertEquals("The photo says ≈641 mm. Check the mark, or shoot again.", MeasureText.photoSays(Tx.EN, 640.5))
        assertEquals("Not in the room — a mark placed after the survey has nothing to measure", MeasureText.notInRoom(Tx.EN))
    }

    @Test fun `a four-corner mark's texts -- its sides' own words on the bar, and the closing check's sentence (port 63 slice Q1)`() {
        // the design's example, made up: front wall 3600 x 2700; FW1 with a sloping bottom; a unit on the floor with no straight side
        val l = 5000.0; val a = 3600.0; val b = 2700.0
        fun size(s: String) = SurveyPrep.size(s, l, a, b)
        val p = SurveyPrep.fresh(l, a, b)
        val w = p.put("front", "W", 1, 600.0, 2970.0, 900.0, 2450.0)
        kotlin.test.assertTrue(Quad.set(w, listOf(pt(600.0, 1670.0), pt(2970.0, 900.0), pt(2970.0, 2450.0), pt(600.0, 2450.0))))
        val u = p.put("floor", "CRP", 1, 400.0, 2200.0, 500.0, 2500.0)
        kotlin.test.assertTrue(Quad.set(u, listOf(pt(400.0, 800.0), pt(2000.0, 500.0), pt(2200.0, 2300.0), pt(700.0, 2500.0))))
        fun label(code: String) = MeasureText.label(Tx.EN, p, code, ::size)
        // LINE 1 is the side's own words, alone
        assertEquals(listOf("bottom side", "right side", "top side", "left side"), listOf("bottom", "right", "top", "left").map { label("FW1.$it") })
        assertEquals("from left corner", label("FW1.l")); assertEquals("sill, from floor, to its nearest corner", label("FW1.s")); assertEquals("reveal depth", label("FW1.p"))
        // w and h are not asked of it: the bar has no words for them
        assertNull(label("FW1.w")); assertNull(label("FW1.h")); assertNull(label("FW1.diag"), "it has a square corner: no diagonal")
        // on the floor the bottom is the back side and the top the front side; with no square corner, the diagonal
        assertEquals(listOf("back side", "right side", "front side", "left side", "diagonal, back-left to front-right"),
            listOf("bottom", "right", "top", "left", "diag").map { label("GCRP1.$it") })
        assertEquals("from left wall, to its nearest corner", label("GCRP1.l")); assertEquals("from back wall, to its nearest corner", label("GCRP1.k"))
        // a wall's diagonal
        val x = p.put("front", "X", 1, 1000.0, 1900.0, 300.0, 800.0)
        kotlin.test.assertTrue(Quad.set(x, listOf(pt(1000.0, 400.0), pt(1800.0, 300.0), pt(1900.0, 700.0), pt(1200.0, 800.0))))
        assertEquals("diagonal, bottom-left to top-right", label("FX1.diag"))
        // the label cell is the mark's code over its type's short name, as for any mark
        assertEquals("FW1\nWindow", MeasureText.cell(Tx.EN, p, w.id)!!.text)
        // THE CLOSING CHECK'S SENTENCE names the side, in its own words, and rounds as a figure does
        assertEquals("The other sides make the bottom side ≈2466 mm. Check the readings, or the corners.", MeasureText.sidesSay(Tx.EN, "bottom side", 2465.6))
        assertEquals("The other sides make the back side ≈2500 mm. Check the readings, or the corners.", MeasureText.sidesSay(Tx.EN, "back side", 2499.5))
        // English first: under Marathi these new texts are still their English, and the measure words the app had keep theirs
        assertEquals("bottom side", MeasureText.label(Tx.MR, p, "FW1.bottom", ::size)); assertEquals("डाव्या कोपऱ्यापासून", MeasureText.label(Tx.MR, p, "FW1.l", ::size))
        assertEquals("The other sides make the bottom side ≈2466 mm. Check the readings, or the corners.", MeasureText.sidesSay(Tx.MR, "bottom side", 2465.6))
        // the menu's two rows and the message after Four corners; and the salvage note
        assertEquals("Four corners", Tx.EN.sp_shape_four_corners); assertEquals("Rectangle", Tx.EN.sp_shape_rectangle)
        assertEquals("Drag a corner. A side near upright or level snaps straight.", Tx.EN.sp_shape_drag_corners)
        assertEquals("Front wall: mark FW1's four corners were unreadable, kept as its box", Tx.EN.sp_file_x_mark_x_shape_unreadable(surface = "Front wall", code = "FW1"))
        // the picked mark's row says "corner" with the key the app already had, Marathi and all
        assertEquals("corner", Tx.EN.sp_corner); assertEquals("कोपरा", Tx.MR.sp_corner)
    }

    @Test fun `the keys slice M2 will read are in the table, in English`() {
        // no reader yet: the face tab's count, the all toggle, the bar's three screen-reader labels, the reference's line
        assertEquals("2 to measure", Tx.EN.sp_measure_x_to_measure("2"))
        assertEquals("all", Tx.EN.sp_measure_all)
        assertEquals("Every figure on this face", Tx.EN.sp_measure_aria_all)
        assertEquals("The measurement before", Tx.EN.sp_measure_aria_previous)
        assertEquals("Skip to the next measurement", Tx.EN.sp_measure_aria_skip)
        assertEquals("Type the figure", Tx.EN.sp_measure_aria_type)
        assertEquals("Reference shown on 2 · Mark, 3 · Measure and 4 · 360", Tx.EN.sp_reference_shown_measure)
        // the key it will replace is untouched, English and Marathi
        assertEquals("Reference shown on 2 · Mark, 3 · List and 4 · 360", Tx.EN.sp_reference_shown_2_mark)
        assertEquals("संदर्भ 2 · मार्क, 3 · यादी आणि 4 · 360 वर दिसतो", Tx.MR.sp_reference_shown_2_mark)
    }
}
