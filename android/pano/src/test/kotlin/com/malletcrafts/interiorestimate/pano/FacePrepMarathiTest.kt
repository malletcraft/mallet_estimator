package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * THE M8 DIM COMPOSITIONS UNDER BOTH LANGUAGES (the M3a fix-7 reason: every Masters map and chipWord pass
 * an unknown value through unchanged, so a lookup wired wrongly prints the same English under every
 * `Tx.EN` pin and leaks Latin only under मराठी). The EN side is pinned byte for byte against cdde242's
 * hand-composed labels, so nothing a measure sheet, a CSV caller or a drawn tag said in English moves.
 */
class FacePrepMarathiTest {

    /** The synthetic room: a door (divided width), a free line, and a full-height column (a step face). */
    private fun room(): FacePrep.Room {
        val r = FacePrep.Room(2677, 2678, 3319)
        val f = r.face("front")
        f.dets.add(FacePrep.Detail("door", 0.30, 0.25, 0.62, 0.9, sx = 0, sy = 1))
        f.dets[0].sw.add(0.5)
        f.lines.add(FacePrep.MLine(doubleArrayOf(0.1, 0.9), doubleArrayOf(0.9, 0.9)))
        return r
    }

    @Test fun `the dims labels compose through the tx -- EN byte for byte cdde242's, MR the register's`() {
        val r = room()
        val en = FacePrep.dims(Tx.EN, r, "front").map { it.label }
        val mr = FacePrep.dims(Tx.MR, r, "front").map { it.label }
        // the box edges and the totals
        assertEquals("E1 · top edge", en[0])
        assertEquals("E1 · वरची कडा", mr[0])
        assertEquals(listOf("Top total", "Right total", "Bottom total", "Left total"),
            en.filter { it.endsWith(" total") })
        assertEquals(listOf("वरची एकूण", "उजवी एकूण", "खालची एकूण", "डावी एकूण"),
            mr.filter { it.endsWith(" एकूण") })
        // a divided width: the word, then the leading-space piece fragment
        assertEquals(true, "Door 1 width piece 1" in en && "Door 1 width piece 2" in en, en.toString())
        assertEquals(true, "दरवाजा 1 रुंदी भाग 1" in mr && "दरवाजा 1 रुंदी भाग 2" in mr, mr.toString())
        // an undivided height stays whole
        assertEquals(true, "Door 1 height" in en, en.toString())
        assertEquals(true, "दरवाजा 1 उंची" in mr, mr.toString())
        // the locator: the name, then the from-word ("दरवाजा 1 जमिनीपासून" — the figure-first order is the TAG's, M3b)
        assertEquals(true, "Door 1 from floor" in en, en.toString())
        assertEquals(true, "दरवाजा 1 जमिनीपासून" in mr, mr.toString())
        // a free line
        assertEquals(true, "Line 1" in en, en.toString())
        assertEquals(true, "रेषा 1" in mr, mr.toString())
    }

    @Test fun `a piece along a step is the step-face fallback, its siblings the edge words`() {
        val r = FacePrep.Room(2600, 3000, 4000)
        r.face("front").steps.add(FacePrep.Step("column", 0.0, 0.0, 0.1, 1.0))   // full height at the left
        val en = FacePrep.dims(Tx.EN, r, "front").map { it.label }
        val mr = FacePrep.dims(Tx.MR, r, "front").map { it.label }
        assertEquals(true, en.any { it.endsWith(" · step face") }, en.toString())
        assertEquals(true, mr.any { it.endsWith(" · स्टेपची कडा") }, mr.toString())
    }

    @Test fun `chipWord gives every chip's word both ways, an unknown label through unchanged`() {
        val chips = (FacePrep.STEP_KINDS + FacePrep.DETAIL_TYPES).map { it.second }
        // EN byte for byte: every chip is its cdde242 label
        for (lab in chips) assertEquals(lab, FacePrep.chipWord(Tx.EN, lab))
        assertEquals("कॉलम", FacePrep.chipWord(Tx.MR, "Column"))
        assertEquals("बीम", FacePrep.chipWord(Tx.MR, "Beam"))
        assertEquals("इतर", FacePrep.chipWord(Tx.MR, "Other step"))
        assertEquals("दरवाजा", FacePrep.chipWord(Tx.MR, "Door"))
        assertEquals("खिडकी", FacePrep.chipWord(Tx.MR, "Window"))
        assertEquals("ओपनिंग", FacePrep.chipWord(Tx.MR, "Opening"))
        assertEquals("इलेक्ट्रिक पॉइंट", FacePrep.chipWord(Tx.MR, "Electrical point"))
        assertEquals("लॉफ्ट", FacePrep.chipWord(Tx.MR, "Loft"))
        assertEquals("unknown kind", FacePrep.chipWord(Tx.MR, "unknown kind"))
    }

    @Test fun `dims under EN and MR differ only in label text, never in keys, calc or site`() {
        // the SurveyDrawMarathiTest figure-items shape: the language moves words, nothing else
        val r = room()
        FacePrep.setSite(r.face("front"), "T:top", 2680)
        val en = FacePrep.dims(Tx.EN, r, "front")
        val mr = FacePrep.dims(Tx.MR, r, "front")
        assertEquals(en.map { it.key }, mr.map { it.key })
        assertEquals(en.map { it.calc }, mr.map { it.calc })
        assertEquals(en.map { it.site }, mr.map { it.site })
    }
}
