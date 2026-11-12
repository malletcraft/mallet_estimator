package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.SheetMarks.Audience
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * WHICH MARKS A DRAWING OF A FACE SHOWS, BY WHO IT IS FOR (port 62 slice L2; DESIGN-port62 section 3.2 and the plan
 * critic's finding 3). FaceWriter saves "<face> survey marks" to the gallery for the handover folder -- "documents
 * other parties consume" -- through the painter the screen uses. Those pictures stay THE ROOM AS SURVEYED: no mark
 * born a wish or Vendor added, no Note, and no state, act, why or note on the marks that are there; a v52 prep's
 * picture is what it always was. The synthetic room: L 3319, W 2678, H 2677.
 *
 * The last three tests read the app's own sources -- :app compiles only in CI -- and hold the wiring: the painter is
 * handed its marks by [SheetMarks.marks] alone, takes its audience with no default, and each caller names the right one.
 */
class SheetMarksTest {

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0

    private val WISH_NOTE = "install UPVC window"
    private val WHY = "the client changed her mind"
    private val DROPPED_NOTE = "ask the office before quoting"
    private val BORN_NOTE = "a new wardrobe here"
    private val NOTE_ALL = "the loft stays as it is"
    private val NOTE_OFFICE = "keep the old shelf for the office"

    private fun mark(id: String, t: String, code: String, vararg extra: Pair<String, Any?>) =
        SurveyPrep.Mark(id, t, code, 100.0, 1300.0, 900.0, 2100.0, linkedMapOf(*extra))

    /** A front wall as a room holds it once wishes are marked on it (slices L3 and L4), and a few things a file may carry. */
    private fun wall(): List<SurveyPrep.Mark> = listOf(
        mark("w1", "W", "FW1"),                                                                 // a surveyed window, as v52 wrote it
        mark("d1", "D", "FD1", "locked" to true),                                                // a surveyed door, locked
        mark("w2", "W", "FW2", "locked" to false, "life" to "wish", "lifeAt" to "2026-11-11T10:30:00+05:30", "lifeAs" to "client",
            "act" to "replace", "note" to WISH_NOTE),                                            // a surveyed window the client wants replaced
        mark("w3", "W", "FW3", "life" to "drop", "lifeAt" to "2026-11-11T10:31:00+05:30", "lifeAs" to "client", "act" to "remove",
            "why" to WHY, "note" to DROPPED_NOTE, "vis" to "office"),                            // a surveyed window: a wish since Dropped
        mark("x1", "X", "FX1", "born" to "wish", "life" to "wish", "lifeAt" to "2026-11-11T10:32:00+05:30", "lifeAs" to "client",
            "cat" to "carpentry", "note" to BORN_NOTE),                                          // born a wish: not in the room
        mark("x2", "X", "FX2", "born" to "vadd", "life" to "cok", "lifeAt" to "2026-11-12T09:00:00+05:30", "lifeAs" to "client"), // a vendor's
        mark("n1", "NOTE", "FNOTE1", "note" to NOTE_ALL),                                        // a Note for the client and the vendors
        mark("n2", "NOTE", "FNOTE2", "cat" to "carpentry", "note" to NOTE_OFFICE, "vis" to "office"), // a Note for the office alone
        mark("e1", "E", "FE1", "sub" to "5a", "life" to "scope", "lifeAt" to "2026-11-13T18:05:09+05:30", "lifeAs" to "staff", "locked" to true),
        mark("z1", "W", "FW9", "born" to "later", "life" to "later"),                            // born a state a later build writes
        mark("k1", "W", "FW8", "later" to mapOf("by" to "v70"), "was" to listOf(1.0, 2.0)),      // a surveyed mark with keys this list does not name
    )

    private val others get() = SheetMarks.marks(wall(), Audience.OTHERS)

    @Test fun `the office is handed every mark as it is -- the very list, so the screen draws what it drew`() {
        val w = wall()
        assertSame(w, SheetMarks.marks(w, Audience.OFFICE))
        assertEquals(listOf(Audience.OFFICE, Audience.OTHERS), Audience.values().toList(), "two audiences, and nothing between them")
    }

    @Test fun `a sheet for others is the room as surveyed -- no mark born a wish or Vendor added, no Note of either audience`() {
        assertEquals(listOf("FW1", "FD1", "FW2", "FW3", "FE1", "FW8"), others.map { it.code }, "in the surface's own order")
        val left = wall().map { it.code } - others.map { it.code }.toSet()
        assertEquals(listOf("FX1", "FX2", "FNOTE1", "FNOTE2", "FW9"), left)
        // which is the rule, mark by mark
        val by = wall().associateBy { it.code }
        assertFalse(SheetMarks.surveyed(by.getValue("FX1")), "born a Client wish: it is not in the room")
        assertFalse(SheetMarks.surveyed(by.getValue("FX2")), "born Vendor added, whatever state it has reached")
        assertFalse(SheetMarks.surveyed(by.getValue("FNOTE1")), "a Note for the client and the vendors is left out too, until Amit rules")
        assertFalse(SheetMarks.surveyed(by.getValue("FNOTE2")), "an office-only Note")
        assertFalse(SheetMarks.surveyed(by.getValue("FW9")), "a born this build does not know is not 'it was there': office-only until named")
        for (c in listOf("FW1", "FD1", "FW2", "FW3", "FE1", "FW8")) assertTrue(SheetMarks.surveyed(by.getValue(c)), c)
        assertTrue(SheetMarks.surveyed(mark("b", "W", "FW7", "born" to "exist")), "born Existing, were it ever written out")
        assertFalse(SheetMarks.surveyed(mark("b", "W", "FW7", "born" to 1)), "a born that is no string")
    }

    @Test fun `every mark on it is drawn as v52 draws it -- the wished window and the Dropped one as the windows that are there`() {
        val src = wall().associateBy { it.id }
        for (e in others) {
            val was = src.getValue(e.id)
            // the same thing in the same place
            assertEquals(listOf(was.id, was.t, was.code, was.x0, was.x1, was.y0, was.y1), listOf(e.id, e.t, e.code, e.x0, e.x1, e.y0, e.y1), e.code)
            // no state: the painter draws v52's outline, no ring, no tag in either language, crowded or not, no bar
            assertEquals(MarkLife.EXIST, MarkLife.lifeOf(e), e.code); assertNull(MarkLife.stroke(MarkLife.lifeOf(e)), e.code)
            for (tx in listOf(Tx.EN, Tx.MR)) for (crowded in listOf(false, true)) assertNull(MarkLife.tag(tx, e, crowded), e.code)
            assertNull(MarkLife.barColour(e), e.code); assertFalse(MarkLife.pastExisting(e), e.code)
            // no act, why, note or audience -- and no key but the ones the list names
            assertEquals(listOf(null, "", "", false), listOf(MarkLife.actOf(e), MarkLife.whyOf(e), MarkLife.noteOf(e.extra), MarkLife.isOffice(e.extra)), e.code)
            assertTrue(SheetMarks.KEPT.containsAll(e.extra.keys), "${e.code}: ${e.extra.keys}")
        }
        // what a mark keeps besides its box: its lock and its sub-kind
        val o = others.associateBy { it.code }
        assertTrue(SurveyPrep.isLocked(o.getValue("FD1"))); assertTrue(SurveyPrep.isLocked(o.getValue("FE1"))); assertFalse(SurveyPrep.isLocked(o.getValue("FW2")))
        assertEquals(mapOf("locked" to false), o.getValue("FW2").extra); assertEquals(emptyMap(), o.getValue("FW3").extra)
        assertEquals(mapOf("sub" to "5a", "locked" to true), o.getValue("FE1").extra); assertEquals(emptyMap(), o.getValue("FW8").extra)
        assertEquals(setOf("locked", "sub"), SheetMarks.KEPT)
        assertEquals(setOf("id", "t", "code", "x0", "x1", "y0", "y1", "locked", "sub"), SurveyPrepFile.MARK_KEYS + SheetMarks.KEPT,
            "S5's own list for a caller who is not staff (slice RD, DESIGN-port62 section 3.11)")
    }

    @Test fun `not a word of a note or a why reaches a sheet for others, and the prep itself is not changed`() {
        fun words(v: Any?): List<String> = when (v) {
            is String -> listOf(v); is Map<*, *> -> v.values.flatMap(::words); is Iterable<*> -> v.flatMap(::words); else -> emptyList() }
        val w = wall()
        val out = SheetMarks.marks(w, Audience.OTHERS)
        val said = out.flatMap { e -> listOf(e.id, e.t, e.code) + words(e.extra) }
        for (word in listOf(WISH_NOTE, WHY, DROPPED_NOTE, BORN_NOTE, NOTE_ALL, NOTE_OFFICE, "wish", "drop", "scope", "client", "staff", "replace", "remove", "office", "carpentry"))
            assertTrue(said.none { word in it }, "'$word' is on the sheet: $said")
        // the marks the sheet was made from still carry everything: a copy was drawn, never the prep
        val by = w.associateBy { it.code }
        assertEquals(WISH_NOTE, MarkLife.noteOf(by.getValue("FW2").extra)); assertEquals("wish", MarkLife.lifeOf(by.getValue("FW2")))
        assertEquals(WHY, MarkLife.whyOf(by.getValue("FW3"))); assertEquals("drop", MarkLife.lifeOf(by.getValue("FW3")))
        assertEquals(listOf("later", "was"), by.getValue("FW8").extra.keys.toList())
        val o = out.associateBy { it.code }
        for (c in listOf("FW2", "FW3", "FE1", "FW8")) assertNotSame(by.getValue(c), o.getValue(c), "$c is a new mark")
        // a mark that carries nothing else IS the mark: nothing to copy
        assertSame(by.getValue("FW1"), o.getValue("FW1")); assertSame(by.getValue("FD1"), o.getValue("FD1"))
    }

    @Test fun `a v52 prep's list is unchanged -- the same marks, in the same order, for both audiences`() {
        val p = SurveyPrep.fresh(L, W, H)
        p.addMarkAt("front", "D", 600.0, 1050.0, W, H, "fd1"); p.addMarkAt("front", "W", 1800.0, 1500.0, W, H, "fw1")
        p.addMarkAt("front", "E", 300.0, 300.0, W, H, "fe1"); p.addMarkAt("floor", "S", 1000.0, 1000.0, W, L, "gs1")
        SurveyPrepEdit.setLocked(p, "fd1", true); SurveyPrepEdit.setLocked(p, "fw1", true); SurveyPrepEdit.setLocked(p, "fw1", false)
        // ... and as the file gives it back
        val q = SurveyPrepFile.decode(SurveyPrepFile.encode(p, L, W, H))!!.prep
        for (prep in listOf(p, q)) for (s in SurveyPrep.SURFACES) {
            val marks = prep.surface(s).marks
            val out = SheetMarks.marks(marks, Audience.OTHERS)
            assertEquals(marks.size, out.size, s)
            for (i in marks.indices) assertSame(marks[i], out[i], "$s ${marks[i].code}: the mark itself, so the picture is what it was")
            assertSame(marks, SheetMarks.marks(marks, Audience.OFFICE))
            assertEquals(SheetMarks.any(prep.surface(s), Audience.OFFICE), SheetMarks.any(prep.surface(s), Audience.OTHERS), s)
            assertEquals(marks.isNotEmpty(), SheetMarks.any(prep.surface(s), Audience.OTHERS), "$s: a marked picture exactly where there was one")
        }
    }

    @Test fun `the rule holds on a prep read back from its file -- states, notes and whys as the file stores them`() {
        val p = SurveyPrep.fresh(L, W, H)
        p.surface("front").marks.addAll(wall())
        p.actual["FW2.w"] = 1210.0; p.actual["FX1.w"] = 600.0
        val q = SurveyPrepFile.decode(SurveyPrepFile.encode(p, L, W, H))!!.prep
        val out = SheetMarks.marks(q.surface("front").marks, Audience.OTHERS)
        assertEquals(listOf("FW1", "FD1", "FW2", "FW3", "FE1", "FW8"), out.map { it.code })
        assertTrue(out.all { MarkLife.tag(Tx.EN, it) == null && MarkLife.stroke(MarkLife.lifeOf(it)) == null && SheetMarks.KEPT.containsAll(it.extra.keys) })
        // the file still holds every key: the sheet reads the prep, it never rewrites it
        assertEquals(wall().map { it.extra.keys.toList() }, q.surface("front").marks.map { it.extra.keys.toList() })
    }

    @Test fun `whether a surface has a marked picture at all -- wishes alone make none for others, a Line or a chain still does`() {
        fun surface(vararg m: SurveyPrep.Mark) = SurveyPrep.fresh(L, W, H).surface("front").also { it.marks.addAll(m) }
        val w = wall().associateBy { it.code }
        val empty = surface()
        assertFalse(SheetMarks.any(empty, Audience.OFFICE)); assertFalse(SheetMarks.any(empty, Audience.OTHERS))
        // only things that are not in the room: the office has a picture, nobody else does
        val wishes = surface(w.getValue("FX1"), w.getValue("FX2"), w.getValue("FNOTE1"), w.getValue("FNOTE2"))
        assertTrue(SheetMarks.any(wishes, Audience.OFFICE)); assertFalse(SheetMarks.any(wishes, Audience.OTHERS))
        // one surveyed mark among them: both
        val mixed = surface(w.getValue("FX1"), w.getValue("FW2"))
        assertTrue(SheetMarks.any(mixed, Audience.OFFICE)); assertTrue(SheetMarks.any(mixed, Audience.OTHERS))
        // a Tape, a Line, an area and a chain are drawn for every audience: they carry no state, and no painter draws their note
        val p = SurveyPrep.fresh(L, W, H)
        p.surface("front").marks.add(w.getValue("FX1")); p.addMeas("front", SurveyMeas.LINE, listOf(doubleArrayOf(100.0, 200.0), doubleArrayOf(900.0, 200.0)), "m1")
        assertTrue(SheetMarks.any(p.surface("front"), Audience.OTHERS))
        val k = SurveyPrep.fresh(L, W, H)
        k.surface("front").marks.add(w.getValue("FNOTE2")); k.addChain("front", "x", listOf(doubleArrayOf(0.0, 500.0), doubleArrayOf(900.0, 500.0)), listOf(null, null), "k1")
        assertTrue(SheetMarks.any(k.surface("front"), Audience.OTHERS))
    }

    // ------------------------------------------------------------------ the Plan tab's A4 sheet (the L2 review, finding 1)

    private val meta = SurveyExport.Meta("AP", "htu", "LR · Living Room", "2026-10-14", SurveyExport.drawn(java.time.LocalDate.of(2026, 10, 19)))

    private fun put(p: SurveyPrep.Prep, s: String, t: String, code: String, x0: Double, x1: Double, y0: Double, y1: Double, vararg extra: Pair<String, Any?>) {
        p.surface(s).marks.add(SurveyPrep.Mark("id-$code", t, code, x0, x1, y0, y1, linkedMapOf(*extra)))
    }

    /** The room as it was surveyed, a v52 prep: a window and a socket on the front wall, a door at the back, a step, a beam and a fan overhead. */
    private fun surveyedRoom(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(L, W, H)
        put(p, "front", "W", "FW1", 782.0, 1953.0, 919.0, 2100.0); put(p, "front", "E", "FE1", 325.0, 375.0, 325.0, 375.0)
        put(p, "back", "D", "BD1", 1500.0, 2300.0, 0.0, 1900.0); put(p, "floor", "S", "GS1", 889.0, 1789.0, 150.0, 450.0)
        put(p, "ceil", "B", "CB1", 600.0, 830.0, 0.0, 3319.0); put(p, "ceil", "F", "CF1", 1325.0, 1475.0, 2125.0, 2275.0)
        p.actual["FW1.s"] = 920.0; p.actual["BD1.w"] = 800.0
        return p
    }

    /** The marks the wish round put into that room that were never in it, by code. */
    private val BORN = listOf("LD1", "RW1", "FO1", "FNOTE1", "BX1", "GX1", "CE1", "CX1")

    /**
     * The same room once wishes are marked on it (slices L3 and L4): the surveyed window to be replaced, the surveyed
     * door's wish since Dropped, the socket in Scope -- and, never in the room, a NEW door on the left wall, a window a
     * vendor added on the right, a new opening and a Note on the front wall, a new unit against the back wall and one
     * on the floor, and two things overhead, with the readings taken for them.
     */
    private fun wishedRoom(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(L, W, H)
        put(p, "front", "W", "FW1", 782.0, 1953.0, 919.0, 2100.0, "life" to "wish", "lifeAt" to "2026-11-11T10:30:00+05:30", "lifeAs" to "client",
            "act" to "replace", "note" to WISH_NOTE)
        put(p, "front", "E", "FE1", 325.0, 375.0, 325.0, 375.0, "life" to "scope", "lifeAt" to "2026-11-13T18:05:09+05:30", "lifeAs" to "staff")
        put(p, "front", "O", "FO1", 2100.0, 2600.0, 0.0, 2100.0, "born" to "wish", "life" to "wish", "lifeAs" to "client")
        put(p, "front", "NOTE", "FNOTE1", 1000.0, 1050.0, 1000.0, 1050.0, "note" to NOTE_OFFICE, "vis" to "office")
        put(p, "back", "D", "BD1", 1500.0, 2300.0, 0.0, 1900.0, "life" to "drop", "act" to "remove", "why" to WHY)
        put(p, "back", "X", "BX1", 200.0, 1200.0, 0.0, 2100.0, "born" to "wish", "life" to "wish", "cat" to "carpentry", "note" to BORN_NOTE)
        put(p, "left", "D", "LD1", 400.0, 1300.0, 0.0, 2100.0, "born" to "wish", "life" to "wish", "lifeAs" to "client", "note" to BORN_NOTE)
        put(p, "right", "W", "RW1", 900.0, 2100.0, 900.0, 2100.0, "born" to "vadd", "life" to "cok", "lifeAs" to "client")
        put(p, "floor", "S", "GS1", 889.0, 1789.0, 150.0, 450.0)
        put(p, "floor", "X", "GX1", 100.0, 700.0, 2000.0, 3000.0, "born" to "wish", "life" to "wish", "cat" to "carpentry")
        put(p, "ceil", "B", "CB1", 600.0, 830.0, 0.0, 3319.0); put(p, "ceil", "F", "CF1", 1325.0, 1475.0, 2125.0, 2275.0)
        put(p, "ceil", "E", "CE1", 1725.0, 1775.0, 725.0, 775.0, "born" to "vadd", "life" to "vadd", "lifeAs" to "vendor")
        put(p, "ceil", "X", "CX1", 200.0, 2400.0, 200.0, 3100.0, "born" to "wish", "life" to "wish", "cat" to "false-ceiling")
        p.actual["FW1.s"] = 920.0; p.actual["BD1.w"] = 800.0
        p.actual["LD1.w"] = 905.0; p.actual["RW1.w"] = 1210.0; p.actual["GX1.w"] = 600.0; p.actual["CE1.cl"] = 1750.0
        return p
    }

    private fun mine(it: SurveyPlan.Item, code: String) = it.key == code || it.key?.startsWith("$code.") == true ||
        (it is SurveyPlan.Text && Regex("(^|[^A-Za-z0-9])" + Regex.escape(code) + "($|[^A-Za-z0-9])").containsMatchIn(it.text))

    @Test fun `the exported A4 sheet is the room as surveyed on all six drawings -- a mark born a wish is on no sheet, a wished one is drawn as it is there`() {
        val wished = wishedRoom(); val surveyed = surveyedRoom()
        fun held(p: SurveyPrep.Prep) = SurveyPrep.SURFACES.map { s -> p.surface(s).marks.map { m -> listOf(m.id, m.t, m.code, m.x0, m.x1, m.y0, m.y1, m.extra) } } to p.actual.toMap()
        val before = held(wished)
        for ((v, _) in SurveyPlan.VIEWS) {
            val sheet = SurveyExport.sheet(v, wished, L, W, H, meta, "Living Room")
            // byte for byte the sheet of the room with no wish round at all: nothing of a state, an act, a why or a note, and no new thing
            assertEquals(SvgWriter.write(SurveyExport.sheet(v, surveyed, L, W, H, meta, "Living Room")), SvgWriter.write(sheet), v)
            for (code in BORN) assertTrue(sheet.drawing.items.none { mine(it, code) }, "$code is on the exported $v sheet")
            // THE SCREEN'S Plan tab is the office's: it draws the prep itself, every mark (until slice L8 draws a wish as a wish)
            val screen = SurveyPlan.drawing(Tx.EN, v, wished, L, W, H, "Living Room")
            val there = BORN.filter { c -> screen.items.any { mine(it, c) } }
            assertTrue(there.isNotEmpty(), "$v: the fixture puts a mark born a wish on every drawing")
        }
        // the door a client WISHES for is the case that matters: on the floor plan a door is a gap through the wall band
        val screen = SurveyPlan.drawing(Tx.EN, "floor", wished, L, W, H)
        assertTrue(screen.withKey("op", "LD1").isNotEmpty() && screen.withKey("swing", "LD1").isNotEmpty(), "the screen draws the wished door's gap and swing")
        val floor = SurveyExport.sheet("floor", wished, L, W, H, meta).drawing
        assertEquals(listOf("FW1", "BD1"), floor.items.filter { it.role == "op" }.map { it.key }, "the sheet's wall is cut where the room's walls are, and nowhere else")
        assertTrue(floor.withKey("swing", "BD1").isNotEmpty(), "the surveyed door, whose wish was Dropped, is the door that is there")
        assertEquals("920", SurveyExport.sheet("front", wished, L, W, H, meta).drawing.dim("FW1.s")?.text, "a surveyed mark keeps its readings")
        // the prep the sheets were drawn from still holds every wish: a copy was drawn
        assertEquals(before, held(wished))
        assertEquals(listOf("wish", "scope", "wish", "exist"), wished.surface("front").marks.map { MarkLife.lifeOf(it) })
    }

    @Test fun `the prep a drawing for others is made from -- itself when nothing is left out, else a new prep that shares all but the marks`() {
        // a v52 prep IS the prep its sheet is drawn from, for both audiences: the sheet is what it always was
        val v52 = surveyedRoom()
        assertSame(v52, SheetMarks.prep(v52, Audience.OTHERS)); assertSame(v52, SheetMarks.prep(v52, Audience.OFFICE))
        SurveyPrepEdit.setLocked(v52, "id-BD1", true)
        assertSame(v52, SheetMarks.prep(v52, Audience.OTHERS), "a lock is the room's own key")
        // the office is handed the prep with its wishes
        val w = wishedRoom()
        assertSame(w, SheetMarks.prep(w, Audience.OFFICE))
        // others: a new prep, each surface's marks what SheetMarks.marks gives, everything else the very same objects
        w.addMeas("front", SurveyMeas.LINE, listOf(doubleArrayOf(100.0, 200.0), doubleArrayOf(900.0, 200.0)), "m1")
        w.addChain("front", "x", listOf(doubleArrayOf(0.0, 500.0), doubleArrayOf(900.0, 500.0)), listOf(null, null), "k1")
        val o = SheetMarks.prep(w, Audience.OTHERS)
        assertNotSame(w, o)
        assertEquals(SurveyPrep.SURFACES.toSet(), o.surfaces.keys)
        for (s in SurveyPrep.SURFACES) {
            val a = w.surface(s); val b = o.surface(s)
            assertEquals(SheetMarks.marks(a.marks, Audience.OTHERS).map { it.code to it.extra }, b.marks.map { it.code to it.extra }, s)
            assertNotSame(a.marks, b.marks, "$s: its own list, so the prep's is never touched")
            assertSame(a.lines, b.lines, s); assertSame(a.set, b.set, s); assertSame(a.extra, b.extra, s)
            assertSame(a.meas, b.meas, s); assertSame(a.chains, b.chains, s)
            assertEquals(listOf(a.measKey, a.chainsKey), listOf(b.measKey, b.chainsKey), s)
        }
        assertEquals(listOf("FW1", "FE1"), o.surface("front").marks.map { it.code }); assertEquals(emptyList(), o.surface("left").marks.map { it.code })
        assertSame(w.actual, o.actual); assertSame(w.extra, o.extra); assertSame(w.extraSurfaces, o.extraSurfaces)
        // a prep whose only difference is a STATE on a surveyed mark is a new prep too: the mark is handed over without it
        val st = surveyedRoom(); val fw1 = st.surface("front").marks[0]; fw1.extra = mapOf("life" to "wish", "act" to "replace")
        val so = SheetMarks.prep(st, Audience.OTHERS)
        assertNotSame(st, so); assertEquals(emptyMap(), so.surface("front").marks[0].extra); assertEquals("wish", MarkLife.lifeOf(fw1))
    }

    /** A :pano source file's code lines, its comment lines left out. */
    private fun pano(name: String): String = File("src/main/kotlin/com/malletcrafts/interiorestimate/pano", name).readLines()
        .filter { l -> l.trimStart().let { !it.startsWith("//") && !it.startsWith("*") && !it.startsWith("/*") } }.joinToString("\n")

    @Test fun `SurveyExport never hands SurveyPlan the prep itself, and in the app only the screen's Plan tab draws from it`() {
        val ex = pano("SurveyExport.kt")
        assertTrue(ex.contains("val room = SheetMarks.prep(prep, SheetMarks.Audience.OTHERS)"), "the sheet's room as surveyed")
        val calls = Regex("""SurveyPlan\s*\.\s*(dims|drawing|infoOf)\(([^)]*)\)""").findAll(ex).map { it.groupValues[1] to it.groupValues[2] }.toList()
        assertEquals(listOf("dims", "drawing"), calls.map { it.first })
        for ((f, args) in calls) {
            assertTrue(Regex("""\broom\b""").containsMatchIn(args), "SurveyPlan.$f is not handed the room as surveyed: ($args)")
            assertFalse(Regex("""\bprep\b""").containsMatchIn(args), "SurveyPlan.$f is handed the prep itself: ($args)")
        }
        assertFalse(ex.contains("Audience.OFFICE"), "an exported sheet is never the office's")
        // in the app a drawing is made in ONE place, the Plan tab on screen (the live language, the prep itself); every FILE is a
        // SurveyExport.Sheet, which PlanExport paints and writes -- it builds no drawing of its own
        val files = app.walk().filter { it.isFile && it.extension == "kt" }.map { it.name }.sorted().toList()
        assertEquals(listOf("SurveyPlanScreen.kt"), files.filter { Regex("""SurveyPlan\s*\.\s*drawing\(""").containsMatchIn(code(it)) })
        assertEquals(listOf("SurveyPrepScreen.kt"), files.filter { Regex("""SurveyExport\s*\.\s*sheet\(""").containsMatchIn(code(it)) })
        assertFalse(Regex("""SurveyPlan\s*\.\s*(dims|drawing|infoOf)\(""").containsMatchIn(code("PlanExport.kt")), "PlanExport makes a drawing of its own")
    }

    // ------------------------------------------------------------------ port 62 slice L6: the Note and the new types

    @Test fun `L6 - a Note placed on the phone, for everybody or for the office alone, is on no picture and no sheet for others -- nor a new thing born a wish, while a surveyed unit keeps its kind`() {
        val at = java.time.Instant.parse("2026-11-11T05:00:00Z")
        val p = SurveyPrep.fresh(L, W, H)
        fun placed(t: String, x: Double, y: Double, id: String, stage: Int, sub: String? = null): SurveyPrep.Mark =
            requireNotNull(PhaseTools.place(p, "front", t, x, y, W, H, id, stage, null, at, sub) { }).mark
        // two Notes, placed as the screen places them and worded through the Mark sheet's own Done: one for the client and
        // the vendors, one for the office alone
        for ((id, words, office) in listOf(Triple("n1", NOTE_ALL, false), Triple("n2", NOTE_OFFICE, true))) {
            placed(MarkLife.NOTE_TYPE, if (office) 2000.0 else 600.0, 1200.0, id, PhaseTools.WISH_LIST)
            val st = MarkSheet.pickOffice(MarkSheet.typeNote(requireNotNull(MarkSheet.open(p, id, fresh = true, hist = 1)), words), office)
            assertEquals(MarkSheet.Outcome.DONE, MarkSheet.apply(p, st, PhaseTools.WISH_LIST, at) { }.outcome)
        }
        // a Kitchen base that is THERE (placed at Survey), and a wardrobe the client wishes for (placed in the Wish list)
        val base = placed("CRP", 1500.0, 400.0, "u1", PhaseTools.SURVEY, "kbase"); val wish = placed("CRP", 700.0, 1100.0, "u2", PhaseTools.WISH_LIST)
        val sf = p.surface("front")
        assertEquals(listOf("FNOTE1 {note=$NOTE_ALL}", "FNOTE2 {note=$NOTE_OFFICE, vis=office}", "FCRP1 {sub=kbase}"), sf.marks.take(3).map { it.code + " " + it.extra })
        assertEquals(listOf("born", "life", "lifeAt", "lifeAs"), wish.extra.keys.toList(), "the wished wardrobe is born a wish: the first kind stores no sub")
        // the office's screen is handed every one; a picture or a sheet for anybody else, the surveyed unit alone -- with its kind
        assertSame(sf.marks, SheetMarks.marks(sf.marks, Audience.OFFICE))
        val out = SheetMarks.marks(sf.marks, Audience.OTHERS)
        assertEquals(listOf("FCRP1"), out.map { it.code }); assertSame(base, out[0], "it carries nothing a sheet leaves out: the mark itself")
        assertEquals("Kitchen base", MarkTypes.displayName(Tx.EN, "front", out[0]))
        assertEquals(listOf(false, false, true, false), sf.marks.map { SheetMarks.surveyed(it) })
        // Notes alone make no "survey marks" picture for others; the office has one
        val notes = SurveyPrep.fresh(L, W, H).surface("back").also { it.marks.addAll(sf.marks.take(2)) }
        assertTrue(SheetMarks.any(notes, Audience.OFFICE)); assertFalse(SheetMarks.any(notes, Audience.OTHERS))
        // not a word of either Note, nor a code of what is not in the room, on any of the six exported sheets
        for ((v, _) in SurveyPlan.VIEWS) {
            val svg = SvgWriter.write(SurveyExport.sheet(v, p, L, W, H, meta, "Living Room"))
            for (w in listOf(NOTE_ALL, NOTE_OFFICE, "FNOTE1", "FNOTE2", "FCRP2")) assertFalse(svg.contains(w), "$v: $w is on a sheet for others")
        }
        assertTrue(SvgWriter.write(SurveyExport.sheet("front", p, L, W, H, meta, "Living Room")).contains("FCRP1"), "the surveyed unit is on the front elevation")
        // the prep the sheets were drawn from still holds all four
        assertEquals(4, sf.marks.size)
    }

    @Test fun `L6 - the Note's pin is painted from the marks the painter was HANDED -- one painter, called from the squared face and the 360, and never by a document`() {
        val files = app.walk().filter { it.isFile && it.extension == "kt" }.map { it.name }.sorted().toList()
        val calls = files.associateWith { Regex("""\bnotePin\(""").findAll(code(it)).count() }.filterValues { it > 0 }
        assertEquals(mapOf("SurveyPrep360.kt" to 1, "SurveyPrepDraw.kt" to 2), calls, "its definition and its one call in drawMarks; the 360's one call")
        val draw = code("SurveyPrepDraw.kt")
        // in drawMarks the pin is drawn inside the loop over `els` -- SheetMarks.marks' answer -- and nowhere else in the painter
        val loop = draw.substringAfter("val els = SheetMarks.marks(o.prep.surface(s).marks, audience)").substringBefore("fun drawGrid(")
        assertEquals(1, Regex("""\bnotePin\(""").findAll(loop).count()); assertTrue(loop.contains("if (MarkLife.isNote(e)) {"))
        assertTrue(draw.contains("fun notePin(c: Canvas, e: SurveyPrep.Mark, x: Float, y: Float, u: Float, sel: Boolean) {"))
        // what the tag says and where it is cut is NotePin.tag's, measured with the paint it is drawn with
        assertTrue(draw.contains("val tag = NotePin.tag(e, u.toDouble()) { t -> pen.measureText(t).toDouble() }"))
        // the 360 is the office's own screen: it is never saved, and FaceWriter and the Plan's export name no Note painter at all
        for (f in listOf("FaceWriter.kt", "PlanExport.kt")) assertFalse(code(f).contains("NotePin") || code(f).contains("notePin"), f)
    }

    // ------------------------------------------------------------------ the wiring, read from the app's sources

    private val app = File("../app/src/main/kotlin/com/malletcrafts/interiorestimate")

    /** A source file's code lines: its `//` and KDoc lines left out, so a comment that names a call is not a call. */
    private fun code(name: String): String {
        assertTrue(app.isDirectory, "the app's sources are beside :pano (${app.absolutePath})")
        return File(app, name).readLines().filter { l -> l.trimStart().let { !it.startsWith("//") && !it.startsWith("*") && !it.startsWith("/*") } }.joinToString("\n")
    }

    /** The third argument of every `SurveyPrepDraw.drawMarks(` call in [src]: the audience it names. */
    private fun audiences(src: String): List<String> =
        Regex("""SurveyPrepDraw\s*\.\s*drawMarks\(\s*[^,]+,\s*[^,]+,\s*([^,]+),""").findAll(src).map { it.groupValues[1].trim() }.toList()

    @Test fun `the painter is handed its marks by SheetMarks alone, and takes its audience with no default`() {
        val draw = code("SurveyPrepDraw.kt")
        assertTrue(draw.contains("fun drawMarks(c: Canvas, tx: Tx, audience: SheetMarks.Audience, s: String, o: Marks,"), "drawMarks' signature")
        assertFalse(Regex("""audience\s*:\s*SheetMarks\.Audience\s*=""").containsMatchIn(draw), "the audience has a default: a caller could forget to say who it draws for")
        // ONE line of the whole painter reads a surface's marks, and it asks SheetMarks for them
        assertEquals(listOf("val els = SheetMarks.marks(o.prep.surface(s).marks, audience)"),
            draw.lines().map { it.trim() }.filter { Regex("""\.marks\b""").containsMatchIn(it) })
        // ... and that is the audience's ONLY use: a state is drawn from the mark the painter was handed (MarkLife's
        // readers), never from a second switch on who it is for -- there is no branch to forget
        assertEquals(2, Regex("""\baudience\b""").findAll(draw).count(), "the audience is named twice in the painter: drawMarks' parameter, and the one call that asks SheetMarks")
    }

    @Test fun `the saved survey-marks pictures are drawn for others, and the same audience decides whether there is one`() {
        val fw = code("FaceWriter.kt")
        assertEquals(listOf("SheetMarks.Audience.OTHERS"), audiences(fw), "FaceWriter's one drawMarks call")
        assertTrue(fw.contains("if (SheetMarks.any(prep.surface(s), SheetMarks.Audience.OTHERS)) {"), "the picture's gate")
        assertFalse(fw.contains("Audience.OFFICE"), "FaceWriter never draws for the office: its pictures leave the phone")
        assertFalse(Regex("""\.marks\b""").containsMatchIn(fw), "FaceWriter reads a surface's marks itself: the gate and the picture could disagree")
    }

    @Test fun `only the screen draws for the office, and every caller of the painter names its audience`() {
        val files = app.walk().filter { it.isFile && it.extension == "kt" }.map { it.name }.sorted().toList()
        val calls = files.associateWith { audiences(code(it)) }.filterValues { it.isNotEmpty() }
        assertEquals(mapOf("FaceWriter.kt" to listOf("SheetMarks.Audience.OTHERS"),
            "SurveyPrepScreen.kt" to listOf("SheetMarks.Audience.OFFICE", "SheetMarks.Audience.OFFICE")), calls)
        // every call was seen: none is written in a way the reader above does not match
        for (f in files) assertEquals(calls[f]?.size ?: 0, Regex("""SurveyPrepDraw\s*\.\s*drawMarks\(""").findAll(code(f)).count(), f)
        assertEquals(listOf("SurveyPrepScreen.kt"), files.filter { code(it).contains("Audience.OFFICE") }, "who draws a mark's state")
    }
}
