package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The Note's pin (port 62 slice L6), against the approved prototype v61c.2 (index61c2.html, L53: noteTag 2914-2917,
 * paintNote 2918-2926; sp.js's `ISPOINT(e.t)||L53.isNote(e)` at 5082, 5791, 5809, 5960 and 5984). The tag's expected
 * values are what `android/docs/port62/proto-tables.mjs` prints under "L6 noteTag" from the page's own noteTag, with a
 * text measured as its UTF-16 units x 6 u -- the measure this test hands [NotePin.tag] too.
 */
class NotePinTest {

    private fun note(code: String, words: Any?, vararg extra: Pair<String, Any?>) =
        SurveyPrep.Mark("n", MarkLife.NOTE_TYPE, code, 0.0, 50.0, 0.0, 50.0, linkedMapOf<String, Any?>().apply { if (words != null) put("note", words); putAll(extra) })

    /** The tag as the script prints it: head | "words" | office | width in u. */
    private fun tag(e: SurveyPrep.Mark, u: Double): String = NotePin.tag(e, u) { t -> t.length * 6.0 * u }.let { t ->
        val w = t.width / u
        "${t.head} | \"${t.text}\" | ${t.office} | ${if (w == Math.rint(w)) w.toLong().toString() else w.toString()}" }

    @Test fun `the tag is the page's own - the code and its pencil, the first words, cut to 120 u - at u 1 and u 2`() {
        val cases = listOf(
            note("FNOTE1", null) to "FNOTE1 ✎ | \"\" | false | 54",
            note("FNOTE1", "   ") to "FNOTE1 ✎ | \"\" | false | 54",
            note("FNOTE1", "beam") to "FNOTE1 ✎ | \"beam\" | false | 78",
            note("FNOTE1", "ask the PMC") to "FNOTE1 ✎ | \"ask the PMC\" | false | 120",                       // eleven characters: exactly the room there is
            note("FNOTE1", "ask the PMCs") to "FNOTE1 ✎ | \"ask the PM…\" | false | 120",                      // twelve: one too many
            note("FNOTE1", "abcdefghijklmnopqrstuvwx") to "FNOTE1 ✎ | \"abcdefghij…\" | false | 120",          // 24 characters
            note("FNOTE1", "abcdefghijklmnopqrstuvwxy") to "FNOTE1 ✎ | \"abcdefghij…\" | false | 120",         // 25
            note("FNOTE1", "The client wants a full-height wardrobe here") to "FNOTE1 ✎ | \"The client…\" | false | 120",
            note("FNOTE1", "  ask\n\nabout   the\tbeam  over the door ") to "FNOTE1 ✎ | \"ask about…\" | false | 114", // one line; no space before the cut
            note("FNOTE1", "close the door with brickwork") to "FNOTE1 ✎ | \"close the…\" | false | 114",
            note("FNOTE1", "DB to be shifted to the passage", "vis" to "office") to "FNOTE1 ✎ | \"DB to be…\" | true | 120", // the padlock takes 12 u
            note("FNOTE1", "DB", "vis" to "office") to "FNOTE1 ✎ | \"DB\" | true | 78",
            note("FNOTE1", null, "vis" to "office") to "FNOTE1 ✎ | \"\" | true | 66",
            note("CNOTE128", "false ceiling stops at the beam") to "CNOTE128 ✎ | \"false ce…\" | false | 120",
            note("GNOTE1234567890123", "tile") to "GNOTE1234567890123 ✎ | \"…\" | false | 120",                 // a code so long nothing fits: the ellipsis alone
            note("FNOTE1", "beam", "vis" to "cv") to "FNOTE1 ✎ | \"beam\" | false | 78",                        // an audience nobody knows is everybody
            note("FNOTE1", "इथे नवीन वॉर्डरोब हवा आहे, पूर्ण उंचीचा") to "FNOTE1 ✎ | \"इथे नवीन व…\" | false | 120",
        )
        for (u in listOf(1.0, 2.0)) for ((e, want) in cases) assertEquals(want, tag(e, u), "u $u · ${e.extra}")
        assertEquals(17, cases.size)
        // the tag is never wider than 120 u, whatever the words, the code and the scale
        for (u in listOf(0.5, 1.0, 2.0, 3.0)) for ((e, _) in cases) assertTrue(NotePin.tag(e, u) { t -> t.length * 6.0 * u }.width <= NotePin.TAG_MAX * u + 1e-9)
        // WHERE THE PHONE DIFFERS FROM THE PAGE, both because of what a file can hold: words that are not a string are no
        // words here (the page prints the number), and a cut never halves an emoji (the page cuts by UTF-16 units)
        assertEquals("FNOTE1 ✎ | \"\" | false | 54", tag(note("FNOTE1", 12345), 1.0))
        val wide = NotePin.tag(note("FNOTE1", "x".repeat(23) + "😀 and more"), 1.0) { t -> t.length * 2.0 }
        assertEquals("x".repeat(23) + "…", wide.text, "the 24th unit is half of the emoji: it goes with its other half")
    }

    @Test fun `the words go on one line - every run of spaces and line ends one space, none at the ends`() {
        assertEquals("ask about the beam over the door", NotePin.oneLine("  ask\n\nabout   the\tbeam  over the door "))
        assertEquals("", NotePin.oneLine("")); assertEquals("", NotePin.oneLine(" \n\t ")); assertEquals("a", NotePin.oneLine("a")); assertEquals("a b", NotePin.oneLine("a  b"))
        assertEquals("line one line two", NotePin.oneLine("line one\r\nline two")); assertEquals("x y", NotePin.oneLine("﻿x y"))
        // a cut that would leave half a pair drops the whole of it: with room for four units, "ab" + half an emoji + "…"
        // would fit -- the pair goes whole, and the words are "ab…"
        assertEquals("ab…", NotePin.tag(note("FNOTE1", "ab😀cdefgh"), 1.0) { s -> if (s.endsWith(" ")) 0.0 else s.length * 30.0 }.text)
        assertEquals("ab😀…", NotePin.tag(note("FNOTE1", "ab😀cdefgh"), 1.0) { s -> if (s.endsWith(" ")) 0.0 else s.length * 24.0 }.text, "with room for five, the emoji stays whole")
    }

    @Test fun `the pin's sizes are the page's - a 20 dp disc, its ring, the picked ring, the tag beside it, the padlock`() {
        assertEquals(listOf(10.0, 2.0, 3.0, 4.0), listOf(NotePin.R, NotePin.RING, NotePin.SEL_GAP, NotePin.LOUPE_R))
        assertEquals(listOf(12.0, 0.5, 11.0, 4.0, 4.0, 3.0), listOf(NotePin.GLYPH, NotePin.GLYPH_DY, NotePin.TAG_SIZE, NotePin.TAG_DX, NotePin.TAG_DY, NotePin.HALO))
        assertEquals(listOf(120.0, 10.0, 5.0, 4.0, 12.0, 2.0, 2.0, 14.0, 26.0), listOf(NotePin.TAG_MAX, NotePin.LOCK, NotePin.LOCK_DX, NotePin.LOCK_UP, NotePin.LOCK_W, NotePin.HEAD_GAP, NotePin.TEXT_BACK, NotePin.PAD_DX, NotePin.PAD_UP))
        assertEquals(24, NotePin.TAG_CHARS)
        assertEquals(listOf(0xffffff, 0x1d1f1a, 0xe0b040, 0xffc46b, 0xeceee8), listOf(NotePin.DISC, NotePin.INK, NotePin.PICKED, NotePin.LOCK_COLOUR, NotePin.TEXT_COLOUR))
        // the pencil on the disc and in the tag is the one glyph a mark's note badge is
        assertEquals("✎", MarkLife.NOTE_BADGE); assertTrue(NotePin.tag(note("FNOTE7", "x"), 1.0) { 0.0 }.head.endsWith(" ✎"))
    }

    // ------------------------------------------------------------------ a Note is drawn and touched as a spot

    @Test fun `a Note is a spot to the finger - picked round its centre, moved whole, never resized - and still no point`() {
        val all = (SurveyPrep.TYPES.values.flatten().map { it.letter } + "ZZ").distinct()
        assertEquals(listOf("E", "P", "F", "NOTE", "PLB", "LV"), all.filter { SurveyPrep.isSpot(it) }, "the five points and the Note's pin")
        assertEquals(listOf("E", "P", "F", "PLB", "LV"), all.filter { SurveyPrep.isPoint(it) })
        assertTrue(MarkTypes.isPin("NOTE")); assertFalse(SurveyPrep.isPoint("NOTE")); assertFalse(SurveyPrep.isSpot("ZZ")); assertFalse(SurveyPrep.isSpot("CRP"))
        // no measures, no reading, no state -- so nothing a point's figures ask applies to it
        val p = SurveyPrep.fresh(3319.0, 2678.0, 2677.0); for (s in SurveyPrep.SURFACES) for (k in SurveyPrep.SLOTS) p.surface(s).set[k] = true
        val n = p.addMarkAt("front", "NOTE", 1300.0, 1200.0, 2678.0, 2677.0, "n1")
        assertEquals("FNOTE1", n.code); assertEquals(listOf(1275.0, 1325.0, 1175.0, 1225.0), listOf(n.x0, n.x1, n.y0, n.y1))
        assertTrue(SurveyPrep.measures("front", n, 2678.0, 2677.0).isEmpty()); assertNull(p.nextOpen("front", n, null, 2678.0, 2677.0))
        // 2 · Mark: its Drawn carries a spot, so a press within reach of its centre grabs it whole -- and it has no tabs, picked or not
        val d = SurveyPrepTouch.Drawn(n.id, 500.0, 510.0, 300.0, 310.0, spot = 5.0)
        for (sel in listOf<String?>(null, n.id)) {
            val h = SurveyPrepTouch.hit(listOf(d), sel, emptyList(), 512.0, 312.0, 32.0, 2.0)
            assertTrue(h is SurveyPrepTouch.Hit.Mark && h.part == "body", "picked $sel: ${h?.javaClass?.simpleName}")
        }
        assertNull(SurveyPrepTouch.hit(listOf(d), null, emptyList(), 600.0, 305.0, 32.0, 2.0), "well away from the pin: the photo's")
        // 4 · 360: a 16-sided ring round its centre, with the radius a spot is drawn with -- as a point, never four corners
        val v = Pano360.faceView("front", 680.0, 840.0); val f = Pano360.frames(p, 3319.0, 2678.0, 2677.0).getValue("front")
        val poly = assertNotNull(Pano360.markPoly(v, "front", f, n, 2.0))
        val spot = assertNotNull(poly.spot); assertEquals(16, poly.pts.size)
        val c = assertNotNull(Pano360.toS(v, "front", f, 1300.0, 1200.0))
        assertEquals(c[0], spot[0], 1e-6); assertEquals(c[1], spot[1], 1e-6); assertTrue(spot[2] >= Pano360.SPOT_MIN_DP * 2.0)
        assertTrue(hypot(poly.pts[0][0] - spot[0], poly.pts[0][1] - spot[1]) >= Pano360.SPOT_HIT_DP * 2.0 - 1e-9)
        // a unit beside it is still its four corners
        assertNull(assertNotNull(Pano360.markPoly(v, "front", f, p.addMarkAt("front", "CRP", 700.0, 1100.0, 2678.0, 2677.0, "u1"), 2.0)).spot)
    }

    @Test fun `the screens ask isSpot wherever a mark is grabbed or holed, and isPoint only for a point's own figures`() {
        val app = java.io.File("../app/src/main/kotlin/com/malletcrafts/interiorestimate")
        fun code(name: String) = java.io.File(app, name).readLines().filter { l -> l.trimStart().let { !it.startsWith("//") && !it.startsWith("*") && !it.startsWith("/*") } }.joinToString("\n")
        val screen = code("SurveyPrepScreen.kt"); val draw = code("SurveyPrepDraw.kt"); val p3 = code("SurveyPrep360.kt")
        // the press (its Drawn's spot) and the floating trash's box on 2 · Mark; the dimmed hole; the 360's tabs, pressed and drawn
        assertTrue(screen.contains("spot = if (SurveyPrep.isSpot(e.t)) SurveyPrep.SPOT / 2 / sq.mpp * k() else null)"))
        assertTrue(screen.contains("else if (SurveyPrep.isSpot(picked!!.t)) SurveyPrepDraw.spotOf(picked, rr.sq, m, dpx).let { d ->"))
        assertTrue(draw.contains("val hole = if (SurveyPrep.isSpot(sm.t)) spotOf(sm, sq, m, o.dp).let { d ->"))
        assertTrue(p3.contains("if (SurveyPrep.isLocked(e) || SurveyPrep.isSpot(e.t) || !PhaseTools.editable(e, stageS)) emptyList()"))
        assertTrue(p3.contains("if (sm != null && st.tabs && !SurveyPrep.isLocked(sm.second) && !SurveyPrep.isSpot(sm.second.t)) {"))
        assertEquals(0, Regex("""isPoint\(""").findAll(screen).count() + Regex("""isPoint\(""").findAll(p3).count(), "no screen asks isPoint for a grab any more")
        // in the painter a Note is branched off BEFORE the fade's layer and before the points: isPoint is left for the dot and a point's two figures
        val loop = draw.substringAfter("for (e in els.sortedBy {").substringBefore("fun drawGrid(")
        val note = loop.indexOf("if (MarkLife.isNote(e)) {"); val fade = loop.indexOf("if (faded) c.saveLayerAlpha(null, FADE)\n            if (SurveyPrep.isPoint(e.t)) {")
        assertTrue(note in 1 until fade, "the Note's branch stands before the fade and the points ($note, $fade)")
        assertTrue(loop.substring(note, fade).contains("continue"), "and leaves the loop: nothing after it draws a box, a figure or a tab for a Note")
        // on the 360 the pin is drawn where a spot is, before the dot
        assertTrue(p3.contains("if (MarkLife.isNote(e)) {\n                    SurveyPrepDraw.notePin(c, e, x, y, u, sel)"))
        // Tx is not needed by the pin: what it draws is a code, a glyph and the words as typed
        assertEquals("FNOTE1 ✎", NotePin.tag(note("FNOTE1", "x"), 1.0) { 0.0 }.head); assertNotNull(Tx.EN)
    }
}
