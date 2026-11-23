package com.malletcrafts.interiorestimate.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * WHAT THE MESSAGE LINE SAYS WHILE MEASURING, IN ENGLISH (port 63 slices M1 and I1b; DESIGN-imagemeter §4, §18). The
 * measure bar these texts were first written for went in slice I1b, and its label, progress, next-face and photo-estimate
 * lines with it. English first (Amit, 16 Nov 2026): the `sp.measure.*` keys are pinned in English only -- their Marathi
 * waits in android/i18n/mr-pending.tsv, and MrPendingTest holds it to that.
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






    @Test fun `what the message line says while measuring - a wall and the room finished, the check with no figure, the 360, a wish`() {
        val p = room()
        assertEquals("Front wall · all 9 measured ✓", MeasureText.faceDone(Tx.EN, "front", 9))
        assertEquals("Room measured · all 31 ✓", MeasureText.roomDone(Tx.EN, 31))
        assertEquals("This reading does not match the mark. Shoot again, or ▸ to keep it.", MeasureText.checkReading(Tx.EN))
        assertEquals("D2 1203 mm — not used: readings are taken on the wall's own tab", MeasureText.on360(Tx.EN, 1203))
        assertTrue(MeasureText.notInRoom(Tx.EN).isNotBlank())
        // the face tab's count, and the next-figure slot's two descriptions (its count is in its description only)
        assertEquals("2 to measure", Tx.EN.sp_measure_x_to_measure("2"))
        assertEquals("Next figure to measure · 7 left", Tx.EN.sp_measure_aria_next_x("7"))
        assertEquals("Keep this reading · nothing else left on this wall", Tx.EN.sp_measure_aria_keep)
        assertEquals("Reference shown on 2 · Mark and 3 · 360", Tx.EN.sp_reference_shown_mark)
        assertTrue(p.surface("front").marks.isNotEmpty())
    }
}
