package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
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

    // ---------------------------------------------------------------- the M3b block (port 61c slice M3b)
    // Every Masters-backed lookup this slice adds, each against the exact strings the served prototype's own text
    // layer (vis()) answered in §26's derivation -- a wrong map or a wrong identity prints the same English under
    // every Tx.EN pin and leaks Latin only under मराठी.

    @Test fun `M3b -- a salvage note fills the surface and its line nominative, as the prototype renders it`() {
        val p = room()
        val o = SurveyPrepFile.encode(p, L, W, H)
        @Suppress("UNCHECKED_CAST") val w = (o["w"] as Map<String, Any?>).toMutableMap()
        @Suppress("UNCHECKED_CAST") val front = (w["front"] as Map<String, Any?>).toMutableMap()
        front["bottom"] = "oops"; w["front"] = front
        val d = assertNotNull(SurveyPrepFile.decode(o.toMutableMap().apply { put("w", w) }))
        // the folder's finalised Marathi has no genitive before the name (§26's page diff): the nominative fill
        assertEquals("समोरील भिंत: जमिनीची रेषा वाचता आले नाही, पुन्हा ठेवले", d.problems.single()(Tx.MR))
        assertEquals("Front wall: its floor line was unreadable, placed again", d.problems.single()(Tx.EN))
        noLatin(d.problems.single()(Tx.MR), "the salvage note")
    }

    @Test fun `M3b -- the 360 caption, the placing refusal and the chain busy refusal fill their surface through Masters`() {
        assertEquals("4 · 360 · फिरून पाहण्यासाठी ओढा · समोरील भिंत", Pano360.caption(Tx.MR, "front"))
        assertEquals("4 · 360 · drag to look round · Front wall", Pano360.caption(Tx.EN, "front"))
        // the Marathi restructures to "{surfName}: इथे {n} लावता येत नाही" -- the key's own shape (vis())
        assertEquals("जमीन: इथे स्विच प्लेट लावता येत नाही", Pano360.placeRefusal(Tx.MR, "front", "floor", "S"))
        assertEquals("A Switch plate cannot go on the floor", Pano360.placeRefusal(Tx.EN, "front", "floor", "S"))
        assertEquals("छत: इथे पायरी लावता येत नाही", Pano360.cannotGo(Tx.MR, "Step", "ceil"))
        assertEquals("आधी साखळी पूर्ण करा (समोरील भिंत) — ✓ किंवा डबल टॅप", SurveyChain.busyText(Tx.MR, "front"))
        assertEquals("Finish the chain on the front wall first — ✓ or a double tap", SurveyChain.busyText(Tx.EN, "front"))
        assertEquals("आधी या पृष्ठभागाच्या रेषा सेट करा", Pano360.noLines(Tx.MR))
    }

    @Test fun `M3b -- a list row's room tail, the eye's words and the reference aria fill their surface through Masters`() {
        val p = room()
        val t = p.addMeas("front", SurveyMeas.TAPE, listOf(doubleArrayOf(0.0, 1200.0), doubleArrayOf(1234.0, 1200.0)), "t")
        assertEquals("टेप · 1234 mm · फोटोवरून · समोरील भिंत", SurveyMeas.listName(Tx.MR, "front", t, true))
        assertEquals("टेप · 1234 mm · फोटोवरून", SurveyMeas.listName(Tx.MR, "front", t, false))
        val k = p.addChain("front", "x", mutableListOf(doubleArrayOf(0.0, 1200.0), doubleArrayOf(905.0, 1200.0),
            doubleArrayOf(3310.0, 1200.0)), listOf(null, null, null), "k1")
        assertEquals("साखळी · आडवी · 2 भाग · समोरील भिंत", SurveyChain.listName(Tx.MR, "front", k, true))
        assertEquals("Chain · horizontal · 2 parts · Front wall", SurveyChain.listName(Tx.EN, "front", k, true))
        assertEquals("बाजूंवर संदर्भ लपवला — तरीही तो समोरील भिंत सरळ करतो", SurveyRef.shownSaid(Tx.MR, "front", false))
        assertEquals("संदर्भ 2 · मार्क, 3 · यादी आणि 4 · 360 वर दिसतो", SurveyRef.shownSaid(Tx.MR, "front", true))
        // the card's own pairing: sp.aria.lock_x over sp.the_x_reference ("समोरील भिंत संदर्भ लॉक करा", vis())
        assertEquals("समोरील भिंत संदर्भ लॉक करा", SurveyRef.lockLabel(Tx.MR, "front", false))
        assertEquals("Lock the front wall reference", SurveyRef.lockLabel(Tx.EN, "front", false))
        assertEquals("Unlock the front wall reference", SurveyRef.lockLabel(Tx.EN, "front", true))
        assertEquals("समोरील भिंत संदर्भ", SurveyRef.name(Tx.MR, "front"))
        for (s in SurveyPrep.SURFACES) {
            noLatin(SurveyRef.name(Tx.MR, s), "reference name $s")
            noLatin(SurveyChain.busyText(Tx.MR, s), "chain busy $s")
        }
    }
}
