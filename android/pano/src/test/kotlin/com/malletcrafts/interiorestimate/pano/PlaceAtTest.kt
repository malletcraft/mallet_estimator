package com.malletcrafts.interiorestimate.pano

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A tapped mark goes where the finger is (v52 slice 7, sp.js addElAt): the type's default size centred on the
 * tap, slid inside the face; a point stays a 50 mm spot centred on the tap. The synthetic room: L 3319,
 * W 2678, H 2677, so the front wall is 2678 x 2677 and the floor 2678 x 3319.
 */
class PlaceAtTest {

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0

    private fun box(t: String, s: String, x: Double, y: Double, a: Double = W, b: Double = H) =
        SurveyPrep.placeAt(t, s, x, y, a, b).toList()

    @Test fun `centred on the tap`() {
        // a Door is 900 x 2100: centred at (1300, 1200) it spans 850..1750 and 150..2250
        assertEquals(listOf(850.0, 1750.0, 150.0, 2250.0), box("D", "front", 1300.0, 1200.0))
        // a Window 1200 x 1200 at (1000, 1500)
        assertEquals(listOf(400.0, 1600.0, 900.0, 2100.0), box("W", "front", 1000.0, 1500.0))
        // a Switch plate 200 x 90 at (600, 1150)
        assertEquals(listOf(500.0, 700.0, 1105.0, 1195.0), box("S", "front", 600.0, 1150.0))
        // the floor's Step is 900 x 300
        assertEquals(listOf(850.0, 1750.0, 1500.0, 1800.0), box("S", "floor", 1300.0, 1650.0, W, L))
    }

    @Test fun `slid back inside the face`() {
        // a Door tapped 100 mm from the left corner, at the floor: it starts at the corner and stands on the floor
        assertEquals(listOf(0.0, 900.0, 0.0, 2100.0), box("D", "front", 100.0, 50.0))
        // and near the right corner, high up: against the corner and the ceiling
        assertEquals(listOf(W - 900, W, H - 2100, H), box("D", "front", 2650.0, 2600.0))
        // a Column spans the wall's height wherever it is tapped; a Beam and a Loft its width
        assertEquals(listOf(1150.0, 1450.0, 0.0, H), box("C", "front", 1300.0, 2000.0))
        assertEquals(listOf(0.0, W, H - 400 - 300, H - 300), box("B", "front", 900.0, H - 500).let { listOf(it[0], it[1], it[2], it[3]) })
        assertEquals(listOf(0.0, W), box("L", "front", 300.0, 2400.0).subList(0, 2))
    }

    @Test fun `clamped on a wall smaller than the type`() {
        // a 600 x 1800 wall: a 1200 x 1200 Window is cut to the wall's width and kept on it
        assertEquals(listOf(0.0, 600.0, 300.0, 1500.0), box("W", "front", 100.0, 900.0, 600.0, 1800.0))
        // a 2100 mm Door on a 1500 mm high wall is cut to its height
        assertEquals(listOf(0.0, 900.0, 0.0, 1500.0), box("D", "front", 0.0, 1400.0, 1000.0, 1500.0))
        // a Switch plate on a 150 x 60 face: 150 x 60
        assertEquals(listOf(0.0, 150.0, 0.0, 60.0), box("S", "front", 75.0, 30.0, 150.0, 60.0))
    }

    @Test fun `points stay 50 x 50 centred on the tap`() {
        for ((t, s) in listOf("E" to "front", "P" to "front", "F" to "front", "E" to "floor", "P" to "floor", "F" to "ceil", "E" to "ceil")) {
            val b = box(t, s, 1234.0, 987.0, W, if (SurveyPrep.isPlan(s)) L else H)
            assertEquals(listOf(1209.0, 1259.0, 962.0, 1012.0), b, "$t on $s")
        }
        // at the very corner a spot is kept whole on the face: its centre 25 mm in
        assertEquals(listOf(0.0, 50.0, 0.0, 50.0), box("E", "front", -10.0, 5.0))
        assertEquals(listOf(W - 50, W, H - 50, H), box("E", "front", W + 10, H))
    }

    @Test fun `addMarkAt numbers as addMark does`() {
        val p = SurveyPrep.fresh(L, W, H)
        val a = p.addMarkAt("front", "D", 1300.0, 1200.0, W, H, "a")
        val b = p.addMark("front", "D", W, H, "b")
        val c = p.addMarkAt("front", "D", 400.0, 1200.0, W, H, "c")
        assertEquals(listOf("FD1", "FD2", "FD3"), listOf(a.code, b.code, c.code))
        assertEquals("GE1", p.addMarkAt("floor", "E", 100.0, 100.0, W, L, "d").code)
        assertEquals(1300.0, (a.x0 + a.x1) / 2)
        assertEquals(3, p.surface("front").marks.size)
    }
}
