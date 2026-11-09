package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * THE WORDS DRAWN ON THE PHOTO under मराठी (port 61c slice M3b) -- the canvas-metrics check. Three parts:
 * (a) a byte-exact pin per DRAWN word under Tx.MR -- the eight boundary-line names, the reference's संदर्भ tag and
 * the chain total at each src, the " d" marker pinned LATIN (the prototype's own screen draws "3310 d" under मराठी;
 * critic note 4) and " लेझर" pinned Devanagari; (b) every drawn tag that holds only a code and figures is the SAME
 * BYTES in every language -- tapeTag, lineTag, segFigure and zoomText take no Tx at all since M3b, which is the
 * strongest possible "the Devanagari figure fits" guarantee, and the pins here keep them wordless; (c) for the words
 * that do change, a width proxy (MR code points ≤ EN + 6, characters only Devanagari ∪ ASCII ∪ the drawn glyph set --
 * a mojibake guard) and the placement rule: SurveyMeas.tapeTagAt keeps a text 1.6 × the English width inside the
 * canvas (it is pure, so this is a real test, not a budget). The RENDERING itself has one proof, the phone (the M3a
 * review's rule); the prototype screenshots in §26's derivation are its preview.
 */
class SurveyDrawMarathiTest {

    private val latin = Regex("[A-Za-z]")
    private val deva = Regex("[ऀ-ॿ]")

    // ------------------------------------------------------------ (a) the drawn words, byte-exact under Tx.MR

    @Test fun `the eight boundary-line names draw in Marathi`() {
        // a wall's four: the floor and ceiling lines, the two corners
        assertEquals("जमिनीची रेषा", SurveyPrep.lineName(Tx.MR, "front", "bottom"))
        assertEquals("छताची रेषा", SurveyPrep.lineName(Tx.MR, "front", "top"))
        assertEquals("डावा कोपरा", SurveyPrep.lineName(Tx.MR, "front", "left"))
        assertEquals("उजवा कोपरा", SurveyPrep.lineName(Tx.MR, "front", "right"))
        // a plan's four edges
        assertEquals("मागची कडा", SurveyPrep.lineName(Tx.MR, "floor", "bottom"))
        assertEquals("पुढची कडा", SurveyPrep.lineName(Tx.MR, "floor", "top"))
        assertEquals("डावी कडा", SurveyPrep.lineName(Tx.MR, "floor", "left"))
        assertEquals("उजवी कडा", SurveyPrep.lineName(Tx.MR, "floor", "right"))
        for (s in SurveyPrep.SURFACES) for (k in SurveyPrep.SLOTS)
            assertFalse(latin.containsMatchIn(SurveyPrep.lineName(Tx.MR, s, k)), "line name $s/$k still Latin")
    }

    @Test fun `the reference's drawn tag is संदर्भ with its figures, question marks before a size is typed`() {
        val r = SurveyRef.Ref(true, listOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(1.0, 0.0), doubleArrayOf(1.0, 1.0),
            doubleArrayOf(0.0, 1.0)), 905.0, 2053.0, true, false, emptyMap())
        assertEquals("संदर्भ 905 × 2053", SurveyRef.label(Tx.MR, r))
        assertEquals("Ref 905 × 2053", SurveyRef.label(Tx.EN, r))
        val none = SurveyRef.Ref(true, null, null, null, true, false, emptyMap())
        assertEquals("संदर्भ ? × ?", SurveyRef.label(Tx.MR, none))
    }

    private fun vals(src: String?): SurveyChain.Vals {
        val est = listOf(905.0, 2405.0)
        return when (src) {
            SurveyChain.LASER -> SurveyChain.Vals(est, 3310.0, listOf(null, null), listOf(null, null), -1, 3310.0, src, 3310.0, null, null)
            SurveyChain.DERIVED -> SurveyChain.Vals(est, 3310.0, listOf(900.0, 2410.0), listOf(900.0, 2410.0), -1, 3310.0, src, null, 3310.0, null)
            SurveyChain.MEASURED -> SurveyChain.Vals(est, 3310.0, listOf(900.0, null), listOf(900.0, 2410.0), 1, 3310.0, src, null, 3310.0, null)
            else -> SurveyChain.Vals(est, 3310.0, listOf(null, null), listOf(null, null), -1, null, null, null, null, null)
        }
    }

    @Test fun `the chain total draws लेझर in Marathi, the derived d stays Latin, a bare total and the photo's are figures`() {
        assertEquals("3310 लेझर", SurveyChain.totalText(Tx.MR, vals(SurveyChain.LASER)))
        assertEquals("3310 laser", SurveyChain.totalText(Tx.EN, vals(SurveyChain.LASER)))
        assertEquals("FK1 Σ 3310 लेझर", SurveyChain.totalTag(Tx.MR, "FK1", vals(SurveyChain.LASER)))
        // " d" is pinned LATIN: the prototype's fragment dictionary only admits 3+-letter words, so its own screen
        // draws "3310 d" under मराठी, and a Marathi single-letter marker would be a state it does not draw
        assertEquals("3310 d", SurveyChain.totalText(Tx.MR, vals(SurveyChain.DERIVED)))
        assertEquals("3310 d", SurveyChain.totalText(Tx.EN, vals(SurveyChain.DERIVED)))
        assertEquals("3310", SurveyChain.totalText(Tx.MR, vals(SurveyChain.MEASURED)))
        assertEquals("≈3310", SurveyChain.totalText(Tx.MR, vals(null)))
        assertEquals("FK1 Σ 3310 d", SurveyChain.totalTag(Tx.MR, "FK1", vals(SurveyChain.DERIVED)))
    }

    // ------------------------------------------------------------ (b) code-and-figure tags: the same bytes in every language

    @Test fun `tapeTag, lineTag, segFigure and zoomText hold only codes, figures and glyphs -- no language at all`() {
        // these take no Tx since M3b: byte-equal under English and Marathi by construction, which is the strongest
        // possible "the Devanagari figure fits" guarantee -- there is none to fit
        assertEquals("F-T1 1234 mm", SurveyMeas.tapeTag("F-T1", 1234.4))
        assertEquals("1234 mm", SurveyMeas.tapeTag(null, 1234.4))
        val pts = listOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(1000.0, 0.0))
        assertEquals("F-M1 1000 mm", SurveyMeas.lineTag("F-M1", SurveyMeas.LINE, pts))
        assertEquals("2410 d" to SurveyChain.DERIVED_COLOUR, SurveyChain.segFigure(vals(SurveyChain.MEASURED), 1))
        assertEquals("900" to SurveyChain.MEASURED_COLOUR, SurveyChain.segFigure(vals(SurveyChain.MEASURED), 0))
        assertEquals("≈905" to SurveyChain.PHOTO_COLOUR, SurveyChain.segFigure(vals(null), 0))
        assertEquals("90°", Pano360.zoomText(90.0))
        for (t in listOf(SurveyMeas.tapeTag("F-T1", 1234.4), SurveyMeas.lineTag("F-M2", SurveyMeas.AREA, pts + listOf(doubleArrayOf(500.0, 500.0))),
            SurveyChain.segFigure(vals(SurveyChain.MEASURED), 1).first, Pano360.zoomText(47.0)))
            assertFalse(deva.containsMatchIn(t), "a figure tag grew Devanagari: '$t'")
    }

    // ------------------------------------------------------------ (c) the words that change: width proxy and placement

    /** ASCII, Devanagari, and the glyphs the painters draw beside words. */
    private val allowed = Regex("""^[ -~ऀ-ॿ·×Σ≈°—–]*$""")

    @Test fun `every changed drawn word is near the English width and holds no mojibake`() {
        val r = SurveyRef.Ref(true, listOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(1.0, 0.0), doubleArrayOf(1.0, 1.0),
            doubleArrayOf(0.0, 1.0)), 905.0, 2053.0, true, false, emptyMap())
        val pairs = SurveyPrep.SURFACES.flatMap { s -> SurveyPrep.SLOTS.map { k ->
            SurveyPrep.lineName(Tx.EN, s, k) to SurveyPrep.lineName(Tx.MR, s, k) } } +
            listOf(SurveyRef.label(Tx.EN, r) to SurveyRef.label(Tx.MR, r),
                SurveyChain.totalTag(Tx.EN, "FK1", vals(SurveyChain.LASER)) to SurveyChain.totalTag(Tx.MR, "FK1", vals(SurveyChain.LASER)))
        for ((en, mr) in pairs) {
            assertTrue(mr.codePointCount(0, mr.length) <= en.codePointCount(0, en.length) + 6,
                "drawn Marathi '$mr' is far wider than '$en'")
            assertTrue(allowed.matches(mr), "mojibake or a stray script in drawn '$mr'")
        }
    }

    @Test fun `the tape tag placer keeps a Marathi-wide text inside the canvas`() {
        // tapeTagAt is pure: a text 1.6 × the English width still comes back inside the w x h canvas, whatever the
        // tape's direction -- the real guarantee behind "a longer Marathi word moves its tag instead of clipping"
        val dp = 2.0; val w = 720.0; val h = 780.0; val tw = 1.6 * 80.0
        for ((a, b) in listOf(
            doubleArrayOf(100.0, 300.0) to doubleArrayOf(700.0, 300.0),   // to the right edge
            doubleArrayOf(700.0, 300.0) to doubleArrayOf(10.0, 300.0),    // to the left edge
            doubleArrayOf(400.0, 50.0) to doubleArrayOf(400.0, 775.0),    // plumb, to the bottom
            doubleArrayOf(30.0, 740.0) to doubleArrayOf(690.0, 760.0))) { // a corner-to-corner diagonal
            val k = SurveyMeas.tapeTagAt(a, b, tw, 0.0, dp, w, h)
            assertTrue(k[0] >= 2 * dp && k[0] + tw <= w - 2 * dp, "left ${k[0]} for $tw px on $w px")
            assertTrue(k[1] >= 14 * dp && k[1] <= h - 4 * dp, "baseline ${k[1]}")
        }
    }

    @Test fun `the guide labels Face Prep draws render in Marathi -- and in English byte for byte what they composed before`() {
        // a wall column carried onto the floor plan, its depth known from the plan column (FacePrepTest's room)
        val r = FacePrep.Room(2665, 3336, 4318)
        r.face("floor").steps.add(FacePrep.Step("column", 3036.0 / 3336, 1500.0 / 4318, 1.0, 1800.0 / 4318, site = true))
        r.face("right").steps.add(FacePrep.Step("column", 0.3, 0.0, 0.4, 1.0))
        val g = assertNotNull(FacePrep.plateGuides(r, "floor").singleOrNull())
        assertTrue(g.corrected)
        val en = g.label(Tx.EN)
        val m = assertNotNull(Regex("""^Column 1 · right wall · corrected for (\d+) mm depth$""").matchEntire(en), en)
        assertEquals("कॉलम 1 · उजवी भिंत · ${m.groupValues[1]} mm डेप्थ धरून दुरुस्त", g.label(Tx.MR))
        // the same wall column with no plan column: depth unknown
        val r2 = FacePrep.Room(2665, 3336, 4318)
        r2.face("right").steps.add(FacePrep.Step("column", 0.3, 0.0, 0.4, 1.0))
        val g2 = assertNotNull(FacePrep.plateGuides(r2, "floor").singleOrNull())
        assertFalse(g2.corrected)
        assertEquals("Column 1 · right wall · depth unknown — draw it here to correct", g2.label(Tx.EN))
        assertEquals("कॉलम 1 · उजवी भिंत · डेप्थ माहीत नाही — दुरुस्तीसाठी इथे काढा", g2.label(Tx.MR))
        // the plan step shown on the wall photo
        val wg = assertNotNull(FacePrep.wallGuides(r, "right").singleOrNull())
        val en2 = wg.label(Tx.EN)
        val m2 = assertNotNull(Regex("""^Column 1 · floor plan · (\d+) mm deep$""").matchEntire(en2), en2)
        assertEquals("कॉलम 1 · फ्लोअर प्लॅन · ${m2.groupValues[1]} mm डेप्थ", wg.label(Tx.MR))
        // the step and detail names, and the locator's from-word: the six edges by their measure.* keys, a named
        // mark through app.from_x -- "230 जमिनीपासून" reads figure-first, "दरवाजा 1 पासून" for a mark
        val f = FacePrep.Face()
        f.steps.add(FacePrep.Step("column", 0.0, 0.0, 0.1, 1.0)); f.steps.add(FacePrep.Step("beam", 0.0, 0.0, 1.0, 0.1))
        f.dets.add(FacePrep.Detail("door", 0.3, 0.25, 0.62, 1.0))
        assertEquals("कॉलम 1", FacePrep.stepName(Tx.MR, f, 0)); assertEquals("बीम 1", FacePrep.stepName(Tx.MR, f, 1))
        assertEquals("Column 1", FacePrep.stepName(Tx.EN, f, 0))
        assertEquals("दरवाजा 1", FacePrep.detName(Tx.MR, f, 0)); assertEquals("Door 1", FacePrep.detName(Tx.EN, f, 0))
        assertEquals("डाव्या भिंतीपासून", FacePrep.fromWord(Tx.MR, "left wall"))
        assertEquals("from left wall", FacePrep.fromWord(Tx.EN, "left wall"))
        assertEquals("जमिनीपासून", FacePrep.fromWord(Tx.MR, "floor"))
        assertEquals("दरवाजा 1 पासून", FacePrep.fromWord(Tx.MR, "दरवाजा 1"))
        assertEquals("from Door 1", FacePrep.fromWord(Tx.EN, "Door 1"))
    }
}
