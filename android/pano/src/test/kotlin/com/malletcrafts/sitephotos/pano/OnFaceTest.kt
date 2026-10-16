package com.malletcrafts.sitephotos.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Is a tap on the face (sp.js onFace): within max(25 mm, 1 % of the longer side) past an edge it still is. */
class OnFaceTest {

    @Test fun `inside and on the edges`() {
        assertTrue(SurveyPrep.onFace(2678.0, 2677.0, 1000.0, 1000.0))
        assertTrue(SurveyPrep.onFace(2678.0, 2677.0, 0.0, 0.0))
        assertTrue(SurveyPrep.onFace(2678.0, 2677.0, 2678.0, 2677.0))
    }

    @Test fun `tolerance is 25 mm on a small face`() {
        // longer side 2000: 1 % is 20, so 25 mm counts
        assertTrue(SurveyPrep.onFace(2000.0, 1500.0, -25.0, 700.0))
        assertFalse(SurveyPrep.onFace(2000.0, 1500.0, -25.5, 700.0))
        assertTrue(SurveyPrep.onFace(2000.0, 1500.0, 1000.0, 1525.0))
        assertFalse(SurveyPrep.onFace(2000.0, 1500.0, 1000.0, 1526.0))
    }

    @Test fun `tolerance is 1 % of the longer side on a big face`() {
        // longer side 5000: 50 mm
        assertTrue(SurveyPrep.onFace(5000.0, 2700.0, 5049.0, 100.0))
        assertFalse(SurveyPrep.onFace(5000.0, 2700.0, 5051.0, 100.0))
        assertTrue(SurveyPrep.onFace(2700.0, 5000.0, 100.0, -50.0))
        assertFalse(SurveyPrep.onFace(2700.0, 5000.0, 100.0, -50.5))
        assertFalse(SurveyPrep.onFace(2700.0, 5000.0, Double.NaN, 10.0))
    }

    @Test fun `the refusal names the face`() {
        assertEquals("Tap inside the front wall to place it", SurveyPrep.placeMiss("front"))
        assertEquals("Tap inside the floor to place it", SurveyPrep.placeMiss("floor"))
        assertEquals("Tap inside the ceiling to place it", SurveyPrep.placeMiss("ceil"))
        assertEquals("Tap inside the left wall to place it", SurveyPrep.placeMiss("left"))
    }
}
