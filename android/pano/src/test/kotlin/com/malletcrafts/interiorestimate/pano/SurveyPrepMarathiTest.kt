package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * THE M3a LOOKUPS UNDER मराठी (the M3a review). Every Masters map passes an unknown value through unchanged, so a
 * lookup wired to the wrong map (`Masters.type` where `type_short` is meant, `surface` where `surface_tab` is) or fed
 * the wrong identity (the tab's word where the name is wanted) prints the same English in every `Tx.EN` pin and leaks
 * Latin only under मराठी -- the fault the PL review recorded for BrowseScreens.kt. So each lookup has a hand-typed
 * Marathi pin here, beside the English pins its own test keeps. The synthetic room: L 3319, W 2678, H 2677.
 */
class SurveyPrepMarathiTest {

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0
    private val latin = Regex("[A-Za-z]")
    private fun noLatin(s: String, what: String) = assertTrue(!latin.containsMatchIn(s), "$what still has Latin: '$s'")

    private fun room(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(L, W, H)
        p.addMarkAt("front", "D", 600.0, 1050.0, W, H, "d1")
        p.addMarkAt("front", "E", 300.0, 300.0, W, H, "e1")
        p.addMarkAt("floor", "E", 300.0, 300.0, W, L, "ge1")
        return p
    }

    @Test fun `a mark type's name is Masters type and its short form type_short -- the floor's S is a Step, a wall's a Switch plate`() {
        assertEquals("पायरी", SurveyPrep.typeName(Tx.MR, "floor", "S"))
        assertEquals("स्विच प्लेट", SurveyPrep.typeName(Tx.MR, "front", "S"))
        assertNotEquals(SurveyPrep.typeName(Tx.MR, "floor", "S"), SurveyPrep.typeName(Tx.MR, "front", "S"))
        assertEquals("इलेक्ट्रिक पॉइंट", SurveyPrep.typeName(Tx.MR, "ceil", "E"))
        assertEquals("दरवाजा", SurveyPrep.typeName(Tx.MR, "front", "D"))
        // the short form (a slot, a More ▸ tile) is type_short, never the name
        assertEquals("इलेक्ट्रिक", SurveyPrepPalette.short(Tx.MR, "Electric point"))
        assertEquals("बल्ब", SurveyPrepPalette.short(Tx.MR, "Bulb point"))
        assertEquals("स्विच", SurveyPrepPalette.short(Tx.MR, "Switch plate"))
        assertEquals("दरवाजा", SurveyPrepPalette.short(Tx.MR, "Door"))
        assertNotEquals(SurveyPrepPalette.short(Tx.MR, "Electric point"), SurveyPrepPalette.short(Tx.MR, "Bulb point"))
        for ((kind, ts) in SurveyPrep.TYPES) for (t in ts) {
            val s = if (kind == "wall") "front" else kind
            noLatin(SurveyPrep.typeName(Tx.MR, s, t.letter), "type ${t.name} on $kind")
            noLatin(SurveyPrepPalette.short(Tx.MR, t.name), "short ${t.name}")
        }
    }

    @Test fun `a lock group's name, a tab, the strip's face, a step's label and the checklist labels`() {
        assertEquals("टेप", SurveyPrepEdit.groupName(Tx.MR, SurveyPrepEdit.Group(null, "tapes", false, 1, SurveyPrepEdit.KIND_TAPE)))
        assertEquals("रेषा मार्क", SurveyPrepEdit.groupName(Tx.MR, SurveyPrepEdit.Group(null, "Line marks", false, 1, SurveyPrepEdit.KIND_LINE)))
        assertEquals("साखळ्या", SurveyPrepEdit.groupName(Tx.MR, SurveyPrepEdit.Group(null, SurveyChain.GROUP, false, 1, SurveyPrepEdit.KIND_CHAIN)))
        // a type group names the type (Lock all Door), never its short form
        assertEquals("पायरी", SurveyPrepEdit.groupName(Tx.MR, SurveyPrepEdit.Group("S", "Step", false, 1)))
        assertEquals("स्विच प्लेट", SurveyPrepEdit.groupName(Tx.MR, SurveyPrepEdit.Group("S", "Switch plate", false, 1)))
        assertEquals("इलेक्ट्रिक पॉइंट", SurveyPrepEdit.groupName(Tx.MR, SurveyPrepEdit.Group("E", "Electric point", false, 1)))
        // a tab is the face's SHORT name (surface_tab); List and Plan are common.*
        assertEquals(listOf("समोर", "उजवी", "मागे", "डावी", "जमीन", "छत", "यादी", "प्लॅन"),
            StepRoute.TAB_ROWS.flatten().map { StepRoute.tabName(Tx.MR, it) })
        for (s in SurveyPrep.SURFACES) noLatin(StepRoute.tabName(Tx.MR, s), "tab $s")
        // the strip's face and every text that names a face use its FULL name (surface)
        assertEquals("समोरील भिंत ·", StepRoute.stripFace(Tx.MR, "front"))
        assertEquals("छत ·", StepRoute.stripFace(Tx.MR, "ceil"))
        assertEquals("2 · मार्क", StepRoute.label(Tx.MR, 1)); assertEquals("4 · 360", StepRoute.label(Tx.MR, 3))
        val p = room()
        assertEquals(listOf("डाव्या कोपऱ्यापासून, मध्यापर्यंत", "जमिनीपासून, मध्यापर्यंत"),
            SurveyPrep.measures0("front", p.markById("e1")!!.second, W, H).map { it.what(Tx.MR) })
        assertEquals(listOf("डाव्या भिंतीपासून, मध्यापर्यंत", "मागच्या भिंतीपासून, मध्यापर्यंत"),
            SurveyPrep.measures0("floor", p.markById("ge1")!!.second, W, L).map { it.what(Tx.MR) })
        assertEquals(listOf("रुंदी", "उंची", "डाव्या कोपऱ्यापासून", "रिव्हील डेप्थ"),
            SurveyPrep.measures0("front", p.markById("d1")!!.second, W, H).map { it.what(Tx.MR) })
    }

    @Test fun `a text that names a surface keeps the name nominative, set off by a colon (L2 finalised, 3 Nov 2026)`() {
        // '{s} रेषा लॉक' read "समोरील भिंत रेषा लॉक": two nouns side by side where Marathi wants the genitive (भिंतीच्या),
        // which changes the name itself and no filled placeholder can give. The colon is the idiom of
        // sp.aria.lock_x_four_lines and the sp.file.x_* keys. The English is untouched.
        assertEquals("समोरील भिंत: रेषा लॉक", SurveyPrepEdit.linesSaid(Tx.MR, "front", true))
        assertEquals("जमीन: रेषा अनलॉक", SurveyPrepEdit.linesSaid(Tx.MR, "floor", false))
        assertEquals("समोरील भिंत: संदर्भ लॉक आहे — आधी अनलॉक करा", SurveyRef.lockedSaid(Tx.MR, "front"))
        assertEquals("छत: संदर्भ लॉक आहे — आधी अनलॉक करा", SurveyRef.lockedSaid(Tx.MR, "ceil"))
        assertEquals("Front wall lines locked", SurveyPrepEdit.linesSaid(Tx.EN, "front", true))
        assertEquals("Front wall reference is locked — unlock first", SurveyRef.lockedSaid(Tx.EN, "front"))
        assertEquals("रेषा लॉक आहेत — आधी अनलॉक करा", SurveyPrepEdit.linesRefusal(Tx.MR))
    }

    @Test fun `THIS 360 and UNDO are drawn upper-case through their keys, which leaves the Marathi untouched`() {
        // ⋮'s header and the snackbar's button are upper-cased on the phone as the prototype's CSS and its raw UNDO literal
        // draw them (SurveyPrepScreen.kt menuSizes, SurveyPrepLocks.kt SpSnack); uppercase() is Locale.ROOT's, a no-op on Devanagari
        assertEquals("THIS 360", Tx.EN.sp_this_360.uppercase())
        assertEquals("UNDO", Tx.EN.sp_aria_undo.uppercase())
        assertEquals("हा 360", Tx.MR.sp_this_360.uppercase())
        assertEquals("मागे घ्या", Tx.MR.sp_aria_undo.uppercase())
    }
}
