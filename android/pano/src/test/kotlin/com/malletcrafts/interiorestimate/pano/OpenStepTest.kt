package com.malletcrafts.interiorestimate.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * THE STEP A FACE OPENS ON (port 63 slice I1c; DESIGN-imagemeter §21, P1). Amit, after using 0.3.345: "every time i open
 * a foto, it always start from 1 - lines. its annoying. once lines are set, I dont want this as default screen but i
 * should be able to change line later on as well if required." Survey prep opens a face -- and switches to one -- on
 * 2 · Mark once its lines are set, on 1 · Lines only while they are not; 1 · Lines stays one tap away.
 *
 * (Test names are plain ASCII: MeasureWalkTest's header says why.)
 */
class OpenStepTest {

    private val L = 3319.0; private val W = 2678.0; private val H = 2677.0
    private val LINES = StepRoute.LINES; private val MARK = StepRoute.MARK; private val PANO = StepRoute.PANO

    /** All four of [s]'s lines placed by hand, where the phone guessed them. */
    private fun SurveyPrep.Prep.place(s: String) { for (k in SurveyPrep.SLOTS) surface(s).set[k] = true }
    private fun set(p: SurveyPrep.Prep, s: String) = PhaseTools.linesSet(p, s, L, W, H)

    @Test fun `a fresh capture - no line set anywhere - opens on 1 Lines, though the phone's own guess already closes`() {
        val p = SurveyPrep.fresh(L, W, H)
        // THE FRAME ALONE IS NOT THE TEST: the four guessed lines close, so "the squared frame exists" on a fresh capture
        assertNotNull(SurveyPrep.frameOf("front", p, L, W, H))
        for (s in SurveyPrep.SURFACES) assertFalse(set(p, s), s)
        assertEquals(LINES, PhaseTools.openedStep("front", PhaseTools.SURVEY, set(p, "front")))
        // three of the four placed is not set
        for (k in SurveyPrep.SLOTS.take(3)) p.surface("front").set[k] = true
        assertFalse(set(p, "front")); assertEquals(LINES, PhaseTools.openedStep("front", PhaseTools.SURVEY, set(p, "front")))
    }

    @Test fun `a face whose lines are set opens on 2 Mark - when survey prep opens and when he switches to its tab`() {
        val p = SurveyPrep.fresh(L, W, H); p.place("front"); p.place("left")
        assertTrue(set(p, "front")); assertTrue(set(p, "left")); assertFalse(set(p, "right"))
        assertEquals(MARK, PhaseTools.openedStep("front", PhaseTools.SURVEY, set(p, "front")))
        // a tab switch: from Mark on the front to the left (set) stays on Mark; to the right (not set) is 1 Lines
        assertEquals(MARK, PhaseTools.openStep("left", MARK, PhaseTools.SURVEY, set(p, "left")))
        assertEquals(LINES, PhaseTools.openStep("right", MARK, PhaseTools.SURVEY, set(p, "right")))
        // ... and from 1 Lines on a face he was fixing to a face whose lines are set: Mark -- Lines is not where a
        // face opens once its lines are set, whatever step he left
        assertEquals(MARK, PhaseTools.openStep("left", LINES, PhaseTools.SURVEY, set(p, "left")))
        // a step key nobody's route has (the old third step's), should a caller still hold it: the face's own rule
        assertEquals(MARK, PhaseTools.openStep("left", "measure", PhaseTools.SURVEY, true))
    }

    @Test fun `1 Lines stays one tap away on the strip whatever a face opened on - a line can always be changed`() {
        val p = SurveyPrep.fresh(L, W, H); p.place("front")
        for (stage in listOf(null, 0, 1, 2, 3, 4, 5)) {
            assertEquals(MARK, PhaseTools.openedStep("front", stage, set(p, "front")), "stage $stage")
            assertTrue(LINES in PhaseTools.routeSteps(stage), "stage $stage")
            assertEquals(PhaseTools.Ask.OPEN, PhaseTools.routeAsk(LINES, stage) { true }, "stage $stage")
            // and once on 1 Lines it stays there until he leaves it: routeMode keeps a set face's Lines
            assertEquals(LINES, PhaseTools.routeMode(LINES, stage) { true }, "stage $stage")
        }
    }

    @Test fun `the Plan tab keeps the step - the face rule applies again on the way back to a face`() {
        val p = SurveyPrep.fresh(L, W, H); p.place("front")
        assertFalse(set(p, "plan"))
        for (from in listOf(LINES, MARK, PANO)) assertEquals(from, PhaseTools.openStep("plan", from, PhaseTools.SURVEY, false), from)
        assertEquals(MARK, PhaseTools.openStep("front", LINES, PhaseTools.SURVEY, set(p, "front")))
        assertEquals(LINES, PhaseTools.openStep("back", MARK, PhaseTools.SURVEY, set(p, "back")))
    }

    @Test fun `on 3 360 a face tab turns the 360 to that face and the step stays 3 360`() {
        for (ls in listOf(true, false)) assertEquals(PANO, PhaseTools.openStep("right", PANO, PhaseTools.SURVEY, ls))
    }

    @Test fun `after Survey a face whose lines are set still opens on 2 Mark - the phase decides what Mark lets him do, not whether it opens`() {
        val p = SurveyPrep.fresh(L, W, H); p.place("front")
        for (stage in listOf(PhaseTools.WISH_LIST, 2, PhaseTools.ESTIMATION, 4, PhaseTools.APPROVED)) {
            assertEquals(MARK, PhaseTools.openedStep("front", stage, true), "stage $stage")
            assertEquals(MARK, PhaseTools.openStep("front", LINES, stage, true), "stage $stage")
            assertEquals(LINES, PhaseTools.openStep("right", MARK, stage, false), "stage $stage")
        }
        // what 2 Mark lets him do there is the phase's: at Estimation and Client approved nothing is held
        assertFalse(PhaseTools.holds(PhaseTools.ESTIMATION)); assertFalse(PhaseTools.holds(PhaseTools.APPROVED)); assertTrue(PhaseTools.holds(PhaseTools.SURVEY))
    }

    @Test fun `a face whose lines are all placed but do not close is not set - it opens on 1 Lines, and 2 Mark is refused there as before`() {
        val p = SurveyPrep.fresh(L, W, H); p.place("front")
        // the bottom line laid exactly on the top one: placed, and no closed shape
        val sf = p.surface("front"); sf.lines["bottom"] = arrayOf(sf.lines.getValue("top")[0].copyOf(), sf.lines.getValue("top")[1].copyOf())
        sf.lines["left"] = arrayOf(sf.lines.getValue("right")[0].copyOf(), sf.lines.getValue("right")[1].copyOf())
        assertTrue(sf.done())
        if (SurveyPrep.frameOf("front", p, L, W, H) == null) {
            assertFalse(set(p, "front")); assertEquals(LINES, PhaseTools.openedStep("front", PhaseTools.SURVEY, false))
            assertEquals(PhaseTools.Ask.LINES_OPEN, PhaseTools.routeAsk(MARK, PhaseTools.SURVEY) { false })
        }
        // whatever the geometry: openStep never opens 2 Mark on a face it is told is not set
        assertEquals(LINES, PhaseTools.openStep("front", MARK, PhaseTools.SURVEY, false))
    }
}
