package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.MarkSheet.Outcome
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.io.File
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The Mark sheet (port 62 slice L3), against the approved prototype v61c.2 (index61c2.html, L53: openSheet 3012, paint
 * 3023-3060, apply 3071-3088, onClick 3096-3101, onInput 3112-3115). TWO KINDS OF EXPECTED VALUE:
 *  - what the sheet DRAWS, for 30 kinds of item at each of the six stages and unread (21 until slice L6, which gave the
 *    script's host the page's own subsOf and setSub, so the sheet paints KIND, and added nine items: types with
 *    sub-kinds, and the Note), is compared row for row with the
 *    prototype's own painted sheet -- `src/test/resources/marksheet/proto-sheet-rows.txt`, which is the first L3 table
 *    of `android/docs/port62/proto-tables.mjs`, cut from its output as its header says. That script binds the page's
 *    own L53 module to a stub host and prints the sheet's HTML as one row; [row] prints a [MarkSheet.View] in the
 *    same form. At Client approved the phone differs ON PURPOSE (DESIGN-port62 Q-list 6) and that row is pinned here;
 *  - what each tap, keystroke and Done LEAVES -- the sheet's state, the keys written and their order, the undo steps
 *    -- is written out by hand, and each journey is one the same script runs through the page's own click, input and
 *    apply handlers ("L3 journeys"; and, for KIND and a NEW Note, "L6 journeys of the sheet"): the values here are what
 *    it prints.
 */
class MarkSheetTest {

    /** 11 Nov 2026, 10:30:00 IST. */
    private val at: Instant = Instant.parse("2026-11-11T05:00:00Z")
    private val now = "2026-11-11T10:30:00+05:30"
    private val before = "2026-11-10T06:37:09+05:30"
    private val STAGES: List<Int?> = listOf(0, 1, 2, 3, 4, 5, null)

    private fun hex(c: Int) = "#" + c.toString(16).padStart(6, '0')

    /** A [MarkSheet.View] as ONE ROW, the form proto-tables.mjs prints the page's own painted sheet in. */
    private fun row(v: MarkSheet.View): String {
        val out = ArrayList<String>()
        out += v.code + MarkSheet.SEP + v.name
        out += "trade ${hex(v.trade.colour)} ${v.trade.text}"
        v.kind?.let { k -> out += "${k.label} " + k.chips.joinToString(", ") { c -> c.text + (if (c.on) "(on)" else "") + (if (k.enabled) "" else "(off)") } }
        val tb = v.tradeBlock
        if (tb != null) out += "${tb.label} ${hex(tb.button.colour)} ${tb.button.text}${if (tb.enabled) "" else " (off)"}"
        v.state?.let { s ->
            out += s.label
            out += "now ${hex(s.now.colour)} ${s.now.name}${s.now.rest}${s.now.vplus?.let { MarkSheet.SEP + "[$it]" }.orEmpty()}"
            if (s.acts.isNotEmpty()) out += "acts " + s.acts.joinToString(", ") { a -> a.text + (if (a.on) "(on)" else "") + (if (a.enabled) "" else "(off)") }
            for (m in s.moves) out += "move ${m.to} ${hex(m.colour)} ${m.label}" + (m.hint?.let { " ~ $it" } ?: "") + (if (m.on) " (on)" else "") +
                (if (m.enabled) "" else " (off)") + (m.why?.let { w -> " + why[${w.text}]${if (w.enabled) "" else "(off)"} max ${MarkLife.WHY_MAX} ph[${v.whyHint}]" } ?: "")
            (s.gate ?: s.noMove)?.let { out += "gate: $it" }
            s.amber?.let { out += "amb: $it" }
            for (f in s.foots) out += "foot: $f"
        }
        val n = v.note
        out += "${n.label} ${n.count} [${n.text}] ph[${n.placeholder}] ${n.cv}${if (n.office) "" else "(on)"} / ${n.officeOnly}${if (n.office) "(on)" else ""}"
        out += "${v.done} ${if (v.doneEnabled) "on" else "off"}"
        val chips = tb?.chips
        if (tb != null && chips != null) out += "picker ${tb.title}: " + chips.joinToString(", ") { c -> "${c.text} ${hex(c.colour)}${if (c.on) "(on)" else ""}" }
        return out.joinToString(" | ")
    }

    /** A room whose front wall holds ONE item, id "a", from its JSON as the prototype holds it: a mark's seven fields (a Line's four) and every other key in `extra`, in its order. */
    @Suppress("UNCHECKED_CAST")
    private fun prepOf(json: String): SurveyPrep.Prep {
        val o = MiniJson.parse(json) as Map<String, Any?>
        val p = SurveyPrep.fresh(3319.0, 2678.0, 2677.0)
        fun num(k: String) = (o.getValue(k) as Number).toDouble()
        fun rest(vararg own: String) = LinkedHashMap(o.filterKeys { it !in own })
        if (o.containsKey("kind")) {
            val pts = (o.getValue("pts") as List<List<Number>>).map { doubleArrayOf(it[0].toDouble(), it[1].toDouble()) }.toMutableList()
            p.surface("front").meas.add(SurveyPrep.Meas(o["id"] as String, o["kind"] as String, o["code"] as String, pts, rest("id", "kind", "code", "pts")))
        } else p.surface("front").marks.add(SurveyPrep.Mark(o["id"] as String, o["t"] as String, o["code"] as String,
            num("x0"), num("x1"), num("y0"), num("y1"), rest("id", "t", "code", "x0", "x1", "y0", "y1")))
        return p
    }

    private fun markJson(t: String, code: String, extra: String = "") = """{"id":"a","t":"$t","code":"$code","x0":0,"x1":1,"y0":0,"y1":1${if (extra.isEmpty()) "" else ",$extra"}}"""
    private fun window(extra: String = "") = prepOf(markJson("W", "FW1", extra))
    private val wish = """"life":"wish","lifeAt":"$before","lifeAs":"client","act":"replace""""

    /** One sheet, driven as the screen drives it: every tap replaces the state; Done counts its undo steps. */
    private inner class Run(val prep: SurveyPrep.Prep, var stage: Int? = PhaseTools.WISH_LIST) {
        var st: MarkSheet.State? = null
        var undo = 0
        var last: MarkSheet.Applied? = null
        fun open(focusMove: String? = null): Run { st = MarkSheet.open(prep, "a", focusMove); return this }
        fun move(to: String) { st = MarkSheet.pickMove(prep, st!!, stage, to) }
        fun why(to: String, typed: String) { st = MarkSheet.typeWhy(prep, st!!, stage, to, typed) }
        fun act(a: String) { st = MarkSheet.pickAct(prep, st!!, stage, a) }
        fun sub(k: String) { st = MarkSheet.pickSub(prep, st!!, k) }
        fun note(typed: String) { st = MarkSheet.typeNote(st!!, typed) }
        fun office(on: Boolean) { st = MarkSheet.pickOffice(st!!, on) }
        fun trade() { st = MarkSheet.togglePicker(prep, st!!) }
        fun cat(c: String) { st = MarkSheet.pickCat(st!!, c) }
        fun extra(): Map<String, Any?> = prep.surface("front").marks.firstOrNull()?.extra ?: prep.surface("front").meas.first().extra
        /** Done: its outcome, the undo steps taken so far, and the item's keys as they now stand, in order. */
        fun done(): String {
            val r = MarkSheet.apply(prep, st!!, stage, at) { undo++ }
            last = r; st = r.state
            return "${r.outcome} · undo $undo · ${extra()}${if (r.amber) " · amber" else ""}"
        }
        fun view(tx: Tx = Tx.EN): MarkSheet.View = assertNotNull(MarkSheet.view(tx, prep, st!!, stage))
        fun shown(tx: Tx = Tx.EN): String = row(view(tx))
    }

    // ------------------------------------------------------------------ what opens a sheet, and how it opens

    @Test fun `a mark, a Line and an area open a sheet -- a Tape, a chain, the reference and an unknown id open none`() {
        val p = SurveyPrep.fresh(3319.0, 2678.0, 2677.0)
        val f = p.surface("front")
        f.marks.add(SurveyPrep.Mark("m1", "W", "FW1", 0.0, 1.0, 0.0, 1.0))
        fun meas(id: String, kind: String, code: String) = SurveyPrep.Meas(id, kind, code, mutableListOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(9.0, 0.0), doubleArrayOf(9.0, 9.0)))
        f.meas.addAll(listOf(meas("l1", "line", "F-M1"), meas("a1", "area", "F-M2"), meas("t1", "tape", "F-T1")))
        f.chains.add(SurveyPrep.Chain("k1", "FK1", "x", mutableListOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(9.0, 0.0))))
        assertEquals(listOf(true, true, true, false, false, false, false),
            listOf("m1", "l1", "a1", "t1", "k1", SurveyRef.id("front"), "zz").map { MarkSheet.opens(p, it) })
        for (id in listOf("t1", "k1", SurveyRef.id("front"), "zz")) { assertNull(MarkSheet.open(p, id), id); assertNull(MarkSheet.itemOf(p, id), id) }
        // the item is the mark, or the Line / area: one of the two, on its surface
        val m = MarkSheet.itemOf(p, "m1")!!; assertEquals("front", m.s); assertSame(f.marks[0], m.mark); assertNull(m.meas); assertEquals("FW1", m.code)
        val l = MarkSheet.itemOf(p, "a1")!!; assertSame(f.meas[1], l.meas); assertNull(l.mark); assertEquals("F-M2", l.code); assertFalse(l.locked)
    }

    @Test fun `the sheet opens on the item as stored -- no move picked, the act chip on its own act or Replace, the trade General when none`() {
        assertEquals(MarkSheet.State("a", null, null, emptyMap(), "replace", null, "", false, "general", "general", false), MarkSheet.open(window(), "a"))
        assertEquals(MarkSheet.State("a", null, null, emptyMap(), "remove", "remove", "close with brickwork", true, "general", "general", false),
            MarkSheet.open(window(""""note":"close with brickwork","vis":"office","life":"wish","act":"remove""""), "a"))
        // Delete on a wish opens it on Dropped
        assertEquals("drop", MarkSheet.open(window(wish), "a", MarkLife.DROP)!!.focusMove)
        // an act that is none of the four, an audience that is not "office", a cat that is no string or blank: the defaults
        val odd = MarkSheet.open(prepOf(markJson("X", "FX1", """"act":"paint","vis":"cv","cat":"","note":7""")), "a")!!
        assertEquals(listOf<Any?>("replace", null, "", false, "general"), listOf(odd.act, odd.act0, odd.note, odd.office, odd.cat))
        // a cat that is no trade is kept as it came until a trade is picked (it reads General)
        assertEquals("zzz", MarkSheet.open(prepOf(markJson("X", "FX1", """"cat":"zzz"""")), "a")!!.cat)
        assertEquals("tiling", MarkSheet.open(prepOf("""{"id":"a","kind":"area","code":"F-M2","pts":[[0,0],[9,0],[9,9]],"cat":"tiling","note":"skirting too"}"""), "a")!!.cat)
    }

    // ------------------------------------------------------------------ what the sheet draws

    /** The golden: (item name, its JSON, stage -> the row the page paints). */
    private fun golden(): List<Triple<String, String, Map<String, String>>> {
        val text = requireNotNull(javaClass.getResourceAsStream("/marksheet/proto-sheet-rows.txt")) { "the golden is missing" }.readBytes().decodeToString()
        val out = ArrayList<Triple<String, String, LinkedHashMap<String, String>>>()
        for (line in text.lines()) {
            if (line.isBlank()) continue
            if (line.startsWith("       ")) { val t = line.trim(); out.last().third[t.substringBefore(' ')] = t.substringAfter(' ').trimStart() }
            else out += Triple(line.trim().substringBefore(" = {"), "{" + line.substringAfter(" = {"), LinkedHashMap())
        }
        return out
    }

    private val SURVEY_GATE = "gate: Finish the survey first — the stage moves to Client wish list on S5"
    private val APPROVED_GATE = "gate: Approved — changes go through change requests"

    @Test fun `the sheet as drawn, for 30 kinds of item at every stage and unread, is the prototype's own painted sheet row for row -- and at Client approved Survey's shape with the approval lock's words`() {
        val items = golden()
        assertEquals(30, items.size)
        var rows = 0
        for ((name, json, want) in items) {
            assertEquals(listOf("0", "1", "2", "3", "4", "5", "null"), want.keys.toList(), name)
            for (stage in STAGES) {
                val prep = prepOf(json)
                val got = row(assertNotNull(MarkSheet.view(Tx.EN, prep, MarkSheet.open(prep, "a")!!, stage), "$name @ $stage"))
                // CLIENT APPROVED IS THE PHONE'S (Q-list 6): no move, no act chip, no foot -- the sheet Survey draws, with
                // the approval lock's own words where Survey's gate says its line. The prototype falls through to Vendor
                // review's moves there, so its row for stage 5 is NOT the expected one.
                val expected = if (stage == PhaseTools.APPROVED) want.getValue("0").replace(SURVEY_GATE, APPROVED_GATE) else want.getValue(stage.toString())
                assertEquals(expected, got, "$name @ stage $stage")
                rows++
            }
        }
        assertEquals(210, rows)
        // nine of the thirty are slice L6's: seven with KIND -- which the sheet of the switch plate has had since then too -- and two Notes
        assertEquals(8, items.count { it.third.getValue("1").contains(" | KIND ") }); assertEquals(2, items.count { it.second.contains("\"t\":\"NOTE\"") })
        // the golden's own stage-5 rows are what the phone does NOT draw: a wish is offered moves there on the page
        val pageAt5 = items.first { it.first == "a wish to replace" }.third.getValue("5")
        assertTrue(pageAt5.contains("move vrev") && pageAt5.contains("move drop"), pageAt5)
        // ... and a project S5 does not list is given Survey's rules: its sheet is stage 0's
        assertEquals(PhaseTools.SURVEY, PhaseTools.stageOf(null, phoneOnly = true))
        assertEquals(PhaseTools.SURVEY, PhaseTools.stageOf(QuoteStage.Cache(listed = false), phoneOnly = false))
    }

    @Test fun `the moves offered at each of the six stages, unread and not on S5 -- none at Survey, Estimated, Client approved and not on S5`() {
        fun offered(extra: String, stage: Int?): String {
            val p = window(extra)
            return assertNotNull(MarkSheet.view(Tx.EN, p, MarkSheet.open(p, "a")!!, stage)).state!!.moves.joinToString(" ") { it.to + if (it.why != null) "?" else "" }.ifEmpty { "-" }
        }
        val notOnS5 = PhaseTools.stageOf(null, phoneOnly = true)
        val all = STAGES + listOf(notOnS5)
        //                                        Survey  Wish list     Vendor quoted       Estimated  Revisions           Approved  unread              not on S5
        assertEquals(listOf("-", "wish", "-", "-", "-", "-", "wish", "-"), all.map { offered("", it) }, "an Existing mark")
        assertEquals(listOf("-", "drop? exist", "vrev drop?", "-", "vrev drop?", "-", "vrev drop? exist", "-"), all.map { offered(wish, it) }, "a wish on a surveyed mark")
        assertEquals(listOf("-", "drop?", "vrev drop?", "-", "vrev drop?", "-", "vrev drop?", "-"), all.map { offered(""""born":"wish","life":"wish"""", it) }, "a mark born a wish")
        assertEquals(listOf("-", "-", "vok wish? drop?", "-", "vok wish? drop?", "-", "vok wish? drop?", "-"), all.map { offered(""""life":"vrev"""", it) }, "in Vendor review")
        assertEquals(listOf("-", "-", "scope wish drop?", "-", "scope wish drop?", "-", "scope wish drop?", "-"), all.map { offered(""""life":"cok"""", it) }, "Client approved")
        assertEquals(listOf("-", "-", "cok", "-", "cok", "-", "cok", "-"), all.map { offered(""""life":"scope","locked":true""", it) }, "in Scope")
        assertEquals(listOf("-", "wish", "wish", "-", "wish", "-", "wish", "-"), all.map { offered(""""life":"drop","why":"no"""", it) }, "Dropped: its Restore")
        // the gate's line where there is one, and no chip, move or foot beside it; elsewhere why the phase offers none
        fun line(extra: String, stage: Int?): String? { val p = window(extra); val s = MarkSheet.view(Tx.EN, p, MarkSheet.open(p, "a")!!, stage)!!.state!!
            if (s.gate != null) assertEquals(listOf(0, 0, 0), listOf(s.acts.size, s.moves.size, s.foots.size)); assertFalse(s.gate != null && s.noMove != null); return s.gate ?: s.noMove }
        assertEquals("Finish the survey first — the stage moves to Client wish list on S5", line(wish, 0))
        assertEquals("Approved — changes go through change requests", line(wish, 5))
        assertEquals("Client wish · Estimation moves nothing — move the phase back on the project screen", line(wish, 3))
        assertEquals("Existing · a wish is marked in the Wish list phase", line("", 2))
        assertEquals("Vendor review · its next move is in Vendor review", line(""""life":"vrev"""", 1))
        assertNull(line(wish, 1)); assertNull(line("", null))
    }

    @Test fun `under Marathi every text of the sheet is the table's -- and a Line's and an area's placeholder is keyed whole, where the page shows English`() {
        fun mr(json: String, stage: Int?, f: (Run) -> Unit = {}): String = Run(prepOf(json), stage).open().also(f).shown(Tx.MR)
        assertEquals("FW1 · खिडकी | trade #4dabf7 दारे-खिडक्या | स्थिती | now #6b6f68 सध्याचे | move wish #0f766e → ग्राहकाची इच्छा ~ ग्राहकाला यात बदल हवा आहे | " +
            "नोंद 0/280 [] ph[या मार्कवर नोंद (ऐच्छिक)] ग्राहक व व्हेंडर(on) / फक्त ऑफिस | पूर्ण on", mr(markJson("W", "FW1"), 1))
        assertEquals("FW1 · खिडकी | trade #4dabf7 दारे-खिडक्या | स्थिती | now #0f766e ग्राहकाची इच्छा · 10 Nov, 06:37 IST पासून · ठरवणारे: ग्राहक | " +
            "acts ↻ बदला(on), ⇢ हलवा, ⚒ दुरुस्ती, ✕ काढा | move drop #b3261e → वगळले (off) + why[] max 140 ph[का? — किमान 3 अक्षरे] | move exist #6b6f68 ← परत \"सध्याचे\" कडे | " +
            "नोंद 0/280 [] ph[या मार्कवर नोंद (ऐच्छिक)] ग्राहक व व्हेंडर(on) / फक्त ऑफिस | पूर्ण on", mr(markJson("W", "FW1", wish), 1))
        // decided by staff: the prototype's text layer draws it through album.staff, lower-cased (a no-op on Devanagari)
        assertTrue(mr(markJson("W", "FW1", """"life":"vrev","lifeAt":"$before","lifeAs":"staff""""), 2).contains("now #9a6700 व्हेंडर तपासणी · 10 Nov, 06:37 IST पासून · ठरवणारे: स्टाफ | "))
        assertEquals("FX1 · इतर | trade #a0522d सुतारकाम | ट्रेड #a0522d सुतारकाम ▾ (off) | स्थिती | now #6b6f68 सध्याचे | gate: आधी सर्व्हे पूर्ण करा — S5 वर टप्पा \"ग्राहकाची इच्छा-यादी\" वर जातो | " +
            "नोंद 0/280 [] ph[या मार्कवर नोंद (ऐच्छिक)] ग्राहक व व्हेंडर(on) / फक्त ऑफिस | पूर्ण on", mr(markJson("X", "FX1", """"cat":"carpentry","locked":true"""), 0))
        assertEquals("gate: मंजूर झाले — पुढचे बदल \"बदल विनंती\" मधूनच होतात", mr(markJson("W", "FW1", wish), 5).split(" | ").first { it.startsWith("gate") })
        assertTrue(mr(markJson("D", "FD1", """"locked":true"""), 1).contains(" | foot: FD1 लॉक आहे — फक्त त्याची नोंद बदलता येते | "))
        assertTrue(mr(markJson("W", "FW1"), null).contains(" | foot: कोटेशनचा टप्पा अजून वाचलेला नाही — ऑनलाइन आल्यावर S5 ठरवते | "))
        // the picker: "Trade · FX1" and the 13 short names
        assertEquals("picker ट्रेड · FX1: सिव्हिल #b197fc, दारे-खिडक्या #4dabf7, ग्रिल #82c91e, इलेक्ट्रिकल #ffd43b, CCTV व डेटा #7048e8, AC #99e9f2, फॉ. सीलिंग #96f2d7, " +
            "सुतारकाम #a0522d, प्लंबिंग #15aabf, टाइलिंग #ff8787, रंगकाम #63e6be, पडदे #c2255c, सामान्य #adb5bd(on)", mr(markJson("X", "FX1"), 1) { it.trade() }.split(" | ").last())
        // a Line and an area: the page glues their placeholder from three pieces and shows it in English under मराठी; here each is ONE key
        val line = """{"id":"a","kind":"line","code":"F-M1","pts":[[0,0],[9,0]]}"""; val area = """{"id":"a","kind":"area","code":"F-M2","pts":[[0,0],[9,0],[9,9]]}"""
        assertEquals("F-M1 · मार्क केलेली लांबी | trade #adb5bd सामान्य | ट्रेड #adb5bd सामान्य ▾ | नोंद 0/280 [] ph[या लांबीवर नोंद (ऐच्छिक)] ग्राहक व व्हेंडर(on) / फक्त ऑफिस | पूर्ण on", mr(line, 1))
        assertEquals("F-M2 · मार्क केलेले क्षेत्रफळ | trade #adb5bd सामान्य | ट्रेड #adb5bd सामान्य ▾ | नोंद 0/280 [] ph[या क्षेत्रफळावर नोंद (ऐच्छिक)] ग्राहक व व्हेंडर(on) / फक्त ऑफिस | पूर्ण on", mr(area, 1))
        assertEquals(listOf("A note on this length (optional)", "A note on this area (optional)"), listOf(line, area).map { Run(prepOf(it)).open().view().note.placeholder })
        // what a screen reader calls the sheet and its ✕; a Note with no words
        val v = Run(window()).open().view(Tx.MR)
        assertEquals(listOf("मार्क तपशील", "बंद करा"), listOf(v.pane, v.close)); assertEquals("नोंदीत शब्द हवेत — ते टाइप करा, किंवा नोंद हटवा", MarkSheet.needsWords(Tx.MR))
        assertEquals(listOf("Mark sheet", "Close", "A Note needs its words — type them, or delete the Note"), Run(window()).open().view().let { listOf(it.pane, it.close, MarkSheet.needsWords(Tx.EN)) })
    }

    /** A table whose every cell is a marker, as TradesTest and MarkLifeTest ask it: a text read from the tables comes back as ONE cell, whole. */
    private fun probe(): Tx {
        val tab = Tx::class.java.getDeclaredField("tab").apply { isAccessible = true }
        val cells = (tab.get(Tx.EN) as Array<*>).size
        return Tx::class.java.getDeclaredConstructor(com.malletcrafts.interiorestimate.pano.i18n.Lang::class.java, Array<String>::class.java)
            .apply { isAccessible = true }.newInstance(com.malletcrafts.interiorestimate.pano.i18n.Lang.EN, Array(cells) { "⟦$it⟧" })
    }

    @Test fun `every word the sheet shows is a cell of the tables -- the labels, the acts, Done, the audience, each placeholder -- never a literal`() {
        val p = probe(); val cell = Regex("^⟦\\d+⟧$")
        val seen = LinkedHashSet<String>()
        fun one(what: String, text: String) { assertTrue(cell.matches(text), "$what is not one cell of the tables: $text"); seen += text }
        val other = Run(prepOf(markJson("X", "FX1", wish)), null).open().also { it.trade() }
        val v = assertNotNull(MarkSheet.view(p, other.prep, other.st!!, null))
        one("the pane", v.pane); one("Close", v.close); one("Done", v.done); one("the why hint", v.whyHint); one("the name", v.name)
        val tb = v.tradeBlock!!; one("TRADE", tb.label); for (c in tb.chips!!) one("a trade", c.text)
        val s = v.state!!; one("STATE", s.label); one("the state's name", s.now.name)
        for (a in s.acts) one("an act chip", a.text); for (m in s.moves) one("a move", m.label); for (f in s.foots) one("a foot", f)
        assertEquals(4, s.acts.map { it.text }.toSet().size, "four acts, four cells")
        one("NOTE", v.note.label); one("the placeholder", v.note.placeholder); one("Client & vendors", v.note.cv); one("Office only", v.note.officeOnly)
        for (json in listOf("""{"id":"a","kind":"line","code":"F-M1","pts":[[0,0],[9,0]]}""", """{"id":"a","kind":"area","code":"F-M2","pts":[[0,0],[9,0],[9,9]]}""", markJson("NOTE", "FNOTE1", """"note":"n""""))) {
            val q = prepOf(json); one("a placeholder", MarkSheet.view(p, q, MarkSheet.open(q, "a")!!, 1)!!.note.placeholder)
        }
        for (stage in listOf(0, 5)) one("a gate", MarkSheet.view(p, other.prep, other.st!!, stage)!!.state!!.gate!!)
        one("needs words", MarkSheet.needsWords(p))
        assertTrue(seen.size >= 30, "distinct cells: ${seen.size}")
        // an act chip is its glyph, a space and its name -- the same act MarkLife names -- as ONE text of the tables each
        for (tx in listOf(Tx.EN, Tx.MR)) assertEquals(MarkLife.ACTS.map { "${it.glyph} ${MarkLife.actName(tx, it.id)}" }, MarkLife.ACTS.map { MarkSheet.actText(tx, it.id) })
        assertEquals("↻ Replace", MarkSheet.actText(Tx.EN, "paint"), "anything else reads Replace, as actName")
        // STATE is common.state in capitals; the count and the separators are not words
        assertEquals(listOf("STATE", "स्थिती"), listOf(Tx.EN, Tx.MR).map { MarkSheet.view(it, other.prep, other.st!!, 1)!!.state!!.label })
    }

    @Test fun `the now-line - since when and who decided, each only when the mark carries it, and V+ on a mark a vendor added`() {
        fun nowOf(extra: String, tx: Tx = Tx.EN) = Run(window(extra)).open().view(tx).state!!.now.let { "${hex(it.colour)} ${it.name}|${it.rest}|${it.vplus}" }
        assertEquals("#6b6f68 Existing||null", nowOf(""))
        assertEquals("#0f766e Client wish| · since 10 Nov, 06:37 IST · decided by the client|null", nowOf(wish))
        assertEquals("#9a6700 Vendor review| · since 10 Nov, 06:37 IST · decided by staff|null", nowOf(""""life":"vrev","lifeAt":"$before","lifeAs":"staff""""))
        assertEquals("#1864ab Vendor approved| · since 10 Nov, 06:37 IST · decided by the vendor|null", nowOf(""""life":"vok","lifeAt":"$before","lifeAs":"vendor""""))
        assertEquals("#b3261e Dropped| · since 10 Nov, 06:37 IST · decided by the client|V+", nowOf(""""born":"vadd","life":"drop","lifeAt":"$before","lifeAs":"client""""))
        // a stamp that is no time is left out; a who that is none of the four is shown as stored; no stamp, no who: the name alone
        assertEquals("#0f766e Client wish| · decided by architect|null", nowOf(""""life":"wish","lifeAt":"soon","lifeAs":"architect""""))
        assertEquals("#0f766e Client wish| · since 10 Nov, 06:37 IST|null", nowOf(""""life":"wish","lifeAt":"$before""""))
        assertEquals("#0f766e Client wish||null", nowOf(""""life":"wish""""))
        // an Existing mark shows no since even with a stale stamp -- but a stale who is shown, as the page shows it
        assertEquals("#6b6f68 Existing| · decided by the surveyor|null", nowOf(""""lifeAt":"$before","lifeAs":"surveyor""""))
        assertEquals(listOf("since 16 Oct, 06:37 IST", "since 3 Sept, 23:59 IST", "", "", ""),
            listOf("2026-10-16T06:37:09+05:30", "2026-09-03T23:59:59+05:30", "", "soon", null).map { MarkSheet.sinceText(Tx.EN, it) })
        assertEquals("16 Oct, 06:37 IST पासून", MarkSheet.sinceText(Tx.MR, "2026-10-16T06:37:09+05:30"))
        assertEquals(listOf("the surveyor", "the client", "the vendor", "staff", "architect"), listOf("surveyor", "client", "vendor", "staff", "architect").map { MarkSheet.decidedBy(Tx.EN, it) })
        assertEquals(listOf("सर्व्हेयर", "ग्राहक", "व्हेंडर", "स्टाफ"), listOf("surveyor", "client", "vendor", "staff").map { MarkSheet.decidedBy(Tx.MR, it) })
    }

    // ------------------------------------------------------------------ Done: the journeys the page's own handlers were run through

    @Test fun `nothing changed -- Done closes the sheet with no undo step and writes nothing`() {
        for (json in listOf(markJson("W", "FW1"), markJson("W", "FW1", """"note":"n","vis":"office",$wish"""), markJson("X", "FX1", """"cat":"carpentry""""),
            """{"id":"a","kind":"line","code":"F-M1","pts":[[0,0],[9,0]],"cat":"painting","note":"cornice"}""")) for (stage in STAGES) {
            val r = Run(prepOf(json), stage).open(); val was = r.extra()
            assertEquals("UNCHANGED · undo 0 · $was", r.done(), "$json @ $stage"); assertNull(r.st); assertSame(was, r.extra(), "not even the map is replaced")
        }
        // the picker opened and shut, a move picked and put back, the audience picked with no note: still nothing
        val r = Run(prepOf(markJson("X", "FX1"))).open()
        r.trade(); r.trade(); r.move("wish"); r.move("wish"); r.office(true)
        assertEquals("UNCHANGED · undo 0 · {}", r.done())
        // an act chip tapped while -> Client wish was picked, and the move then put back: the mark is still Existing, and an
        // Existing mark takes no act -- nothing changed, no undo step
        val e = Run(window()).open()
        e.move("wish"); e.act("remove"); e.move("wish"); assertEquals(listOf<Any?>(null, "remove"), listOf(e.st!!.to, e.st!!.act))
        assertEquals("UNCHANGED · undo 0 · {}", e.done())
    }

    @Test fun `Existing to Client wish with Remove and a note for the office -- then the act alone -- then Dropped from the delete control -- one undo step each`() {
        val r = Run(window()).open()
        r.move("wish"); assertEquals("wish", r.st!!.to); r.move("wish"); assertNull(r.st!!.to, "a second tap puts it back"); r.move("wish")
        r.act("remove"); assertEquals("remove", r.st!!.act)
        r.note("close with brickwork"); r.office(true)
        assertEquals("FW1 · Window | trade #4dabf7 Doors & win. | STATE | now #6b6f68 Existing | acts ↻ Replace, ⇢ Shift, ⚒ Repair, ✕ Remove(on) | " +
            "move wish #0f766e → Client wish ~ Client wants this changed (on) | NOTE 20/280 [close with brickwork] ph[A note on this mark (optional)] Client & vendors / Office only(on) | Done on", r.shown())
        // the note first, then the move's keys: the prototype's order
        assertEquals("DONE · undo 1 · {note=close with brickwork, vis=office, life=wish, lifeAt=$now, lifeAs=client, act=remove}", r.done())
        assertNull(r.st, "the sheet closes")
        // reopened: the act chip is on the mark's own act
        r.open(); assertEquals(MarkSheet.State("a", null, null, emptyMap(), "remove", "remove", "close with brickwork", true, "general", "general", false), r.st)
        r.act("shift")
        assertEquals("DONE · undo 2 · {note=close with brickwork, vis=office, life=wish, lifeAt=$now, lifeAs=client, act=shift}", r.done())
        // Delete on the wish: the sheet opens on Dropped, and a why typed there picks the move
        r.open(MarkLife.DROP); assertEquals("drop", r.view().focusWhy)
        r.why("drop", "no"); assertNull(r.st!!.to); r.move("drop"); assertNull(r.st!!.to, "the move still needs its why: its button is off")
        r.why("drop", "no budget"); assertEquals("drop", r.st!!.to, "three characters pick it")
        r.why("drop", "n"); assertNull(r.st!!.to, "and a why cut short un-picks it")
        r.why("drop", "  not needed  ")
        assertEquals("FW1 · Window | trade #4dabf7 Doors & win. | STATE | now #0f766e Client wish · since 11 Nov, 10:30 IST · decided by the client | acts ↻ Replace, ⇢ Shift(on), ⚒ Repair, ✕ Remove | " +
            "move drop #b3261e → Dropped (on) + why[  not needed  ] max 140 ph[Why? — at least 3 characters] | move exist #6b6f68 ← Back to Existing | " +
            "NOTE 20/280 [close with brickwork] ph[A note on this mark (optional)] Client & vendors / Office only(on) | Done on", r.shown())
        assertEquals("DONE · undo 3 · {note=close with brickwork, vis=office, life=drop, lifeAt=$now, lifeAs=client, act=shift, why=not needed}", r.done())
        // the Dropped mark's sheet offers its Restore, to what it was born
        r.open()
        assertTrue(r.shown().contains(" | move wish #0f766e ↺ Restore ~ to Client wish | "), r.shown())
        r.move("wish")
        assertEquals("DONE · undo 4 · {note=close with brickwork, vis=office, life=wish, lifeAt=$now, lifeAs=staff, act=shift}", r.done(), "the why goes with the move back")
    }

    @Test fun `a move that needs a why is off until three characters -- and Done is off, and writes nothing, while the move picked still needs it`() {
        val r = Run(window(wish)).open()
        fun drop() = r.view().state!!.moves.first { it.to == "drop" }
        assertFalse(drop().enabled); assertEquals("", drop().why!!.text); assertTrue(drop().why!!.enabled)
        for (typed in listOf("a", "ab", "  ab  ", "   ")) { r.why("drop", typed); assertFalse(drop().enabled, "'$typed'"); r.move("drop"); assertNull(r.st!!.to, "'$typed'") }
        // exactly three characters, once trimmed, are enough
        for (typed in listOf("abc", "  abc  ", "का?")) { r.why("drop", typed); assertTrue(drop().enabled, "'$typed'"); assertNull(r.st!!.to) }
        // typed with no Delete before it, a why does not pick the move: a tap does
        r.why("drop", "too dear"); assertNull(r.st!!.to); assertTrue(drop().enabled)
        r.move("drop"); assertEquals("drop", r.st!!.to); assertTrue(r.view().doneEnabled)
        r.why("drop", "to"); assertNull(r.st!!.to, "cut short again, it is un-picked"); assertTrue(r.view().doneEnabled, "Done is on again: there is no move to wait for")
        // the why box takes 140 characters and no more
        r.why("drop", "x".repeat(200)); assertEquals(140, r.st!!.why.getValue("drop").length)
        // a state that still holds the move with too short a why -- the screen cannot make one -- keeps Done off and writes nothing
        val held = MarkSheet.open(r.prep, "a")!!.copy(to = "drop", why = mapOf("drop" to "no"))
        assertFalse(MarkSheet.view(Tx.EN, r.prep, held, 1)!!.doneEnabled)
        var undo = 0
        val out = MarkSheet.apply(r.prep, held, 1, at) { undo++ }
        assertEquals(listOf<Any?>(Outcome.STAYS, held, 0, "wish"), listOf(out.outcome, out.state, undo, r.extra()["life"]))
        // opened on Dropped (Delete on a wish), its Why box takes the keyboard -- only where there IS a box that takes typing:
        // not at a gate, not at Estimation, not on a locked mark
        assertEquals(listOf("drop", "drop", "drop"), listOf(1, 2, null).map { Run(window(wish), it).open(MarkLife.DROP).view().focusWhy })
        for (stage in listOf(0, 3, 5)) assertNull(Run(window(wish), stage).open(MarkLife.DROP).view().focusWhy, "stage $stage")
        assertNull(Run(window("$wish,\"locked\":true")).open(MarkLife.DROP).view().focusWhy, "a locked mark")
        assertNull(Run(window(wish)).open().view().focusWhy, "opened by its code")
        // a move that takes no why has no box, and a why typed for it, or for a move the phase does not offer, changes nothing
        assertNull(r.view().state!!.moves.first { it.to == "exist" }.why)
        val st = r.st; r.why("exist", "because"); r.why("vrev", "because"); assertEquals(st, r.st)
    }

    @Test fun `Back to Existing takes the act and the stamp away, keeps the note -- and the act chips are not drawn for it`() {
        val r = Run(window(""""note":"n","life":"wish","lifeAt":"$before","lifeAs":"client","act":"shift"""")).open()
        assertEquals(4, r.view().state!!.acts.size)
        r.move("exist"); assertTrue(r.view().state!!.acts.isEmpty()); r.act("repair"); assertEquals("shift", r.st!!.act, "no chip, no pick")
        assertEquals("DONE · undo 1 · {note=n}", r.done())
    }

    @Test fun `the act is written only on Existing to Client wish -- a chip changed beside any other move is not`() {
        val r = Run(window(wish), PhaseTools.VENDOR_REVIEW).open()
        r.act("shift"); r.move("vrev")
        assertEquals(listOf<Any?>("vrev", "shift", "replace"), listOf(r.st!!.to, r.st!!.act, r.st!!.act0))
        assertEquals("DONE · undo 1 · {life=vrev, lifeAt=$now, lifeAs=staff, act=replace}", r.done())
        // at Estimation the sheet offers no move, and an act chip changed alone IS still written -- as the page does (Q-list 9)
        val est = Run(window(wish), PhaseTools.ESTIMATION).open()
        assertTrue(est.view().state!!.moves.isEmpty()); est.act("shift")
        assertEquals("DONE · undo 1 · {life=wish, lifeAt=$before, lifeAs=client, act=shift}", est.done())
        // an Existing mark shows no act chip until -> Client wish is picked, and takes none without it
        val e = Run(window()).open()
        assertTrue(e.view().state!!.acts.isEmpty()); e.act("shift"); assertEquals("replace", e.st!!.act)
        e.move("wish"); assertEquals(listOf("↻ Replace(on)", "⇢ Shift", "⚒ Repair", "✕ Remove"), e.view().state!!.acts.map { it.text + if (it.on) "(on)" else "" })
        e.act("zzz"); assertEquals("replace", e.st!!.act, "an act that is none of the four")
        assertEquals("DONE · undo 1 · {life=wish, lifeAt=$now, lifeAs=client, act=replace}", e.done(), "with no chip touched, Replace")
        // a mark born a wish has nothing to replace: no chip, no act
        val b = Run(window(""""born":"wish","life":"wish"""")).open()
        assertTrue(b.view().state!!.acts.isEmpty()); b.act("shift"); b.note("new wardrobe here")
        assertEquals("DONE · undo 1 · {born=wish, life=wish, note=new wardrobe here}", b.done())
        // a mark past Existing with no act stored: its chip stands on Replace, and Done writes it (apply's actCh)
        val n = Run(window(""""life":"wish"""")).open()
        assertEquals(listOf<Any?>("replace", null), listOf(n.st!!.act, n.st!!.act0))
        assertEquals("DONE · undo 1 · {life=wish, act=replace}", n.done())
    }

    @Test fun `at Client approved Done writes no act -- the sheet draws no act chip there, so a mark with none stored, or one this build does not know, keeps what it has`() {
        // The L3 review (11 Nov 2026). The chip stands on Replace for a mark past Existing whose stored act is absent or none of
        // the four (act = replace, act0 = none), and the phone's own gate draws NO chip at Client approved (Q-list 6): a control
        // that is not drawn must not write. (The page draws Vendor review's sheet there, chips and all, and writes -- the
        // journey "... the same at Survey (stage 0) and at Client approved (stage 5)" of proto-tables.mjs.)
        val bare = """"life":"wish","lifeAs":"client""""
        val n = Run(window(bare), PhaseTools.APPROVED).open()
        assertEquals(listOf<Any?>("replace", null), listOf(n.st!!.act, n.st!!.act0))
        assertEquals(listOf<Any?>(0, 0, "Approved — changes go through change requests"), n.view().state!!.let { listOf(it.acts.size, it.moves.size, it.gate) })
        val was = n.extra()
        assertEquals("UNCHANGED · undo 0 · {life=wish, lifeAs=client}", n.done(), "nothing touched: nothing written, no undo step")
        assertSame(was, n.extra(), "not even the map is replaced"); assertNull(n.st, "the sheet closes")
        // a note typed there is written (provisional, Q-list 6 and 9) -- and the act is still absent
        val t = Run(window(bare), PhaseTools.APPROVED).open(); t.note("ask the client")
        assertEquals("DONE · undo 1 · {life=wish, lifeAs=client, note=ask the client}", t.done())
        // an act this build does not know is not replaced by Replace: untouched, and beside a note
        val u = Run(window(""""life":"wish","act":"closeup""""), PhaseTools.APPROVED).open()
        assertEquals(listOf<Any?>("replace", null), listOf(u.st!!.act, u.st!!.act0))
        assertEquals("UNCHANGED · undo 0 · {life=wish, act=closeup}", u.done())
        u.open(); u.note("n")
        assertEquals("DONE · undo 1 · {life=wish, act=closeup, note=n}", u.done())
        // an Other's trade picked there is written too, and still no act
        val o = Run(prepOf(markJson("X", "FX1", bare)), PhaseTools.APPROVED).open(); o.trade(); o.cat("carpentry")
        assertEquals("DONE · undo 1 · {life=wish, lifeAs=client, cat=carpentry}", o.done())
        // even a state that holds another act -- the screen cannot make one there: pickAct changes nothing -- writes none
        val held = Run(window(wish), PhaseTools.APPROVED).open(); held.act("shift"); assertEquals("replace", held.st!!.act)
        held.st = held.st!!.copy(act = "shift")
        assertEquals("UNCHANGED · undo 0 · {life=wish, lifeAt=$before, lifeAs=client, act=replace}", held.done())
        // EVERY OTHER STAGE IS THE PAGE'S: its apply asks the stage only through the moves, so the act is written at Survey too
        // (where the page, like the phone, draws its gate's line and no chip), at Estimation, and wherever the chips are drawn
        for (stage in listOf(0, 1, 2, 3, 4, null)) {
            assertEquals("DONE · undo 1 · {life=wish, lifeAs=client, act=replace}", Run(window(bare), stage).open().done(), "stage $stage")
            val k = Run(window(""""life":"wish","act":"closeup""""), stage).open(); k.note("n")
            assertEquals("DONE · undo 1 · {life=wish, act=replace, note=n}", k.done(), "stage $stage: an unknown act, beside a note")
        }
    }

    @Test fun `the note is capped at 280 characters, an emoji never halved -- and Office only is kept only beside a note`() {
        val r = Run(window()).open()
        r.note("x".repeat(279) + "😀😀"); r.office(true)
        assertEquals("280/280", r.view().note.count)
        r.done()
        val note = r.extra()["note"] as String
        assertEquals(listOf<Any?>(280, 281, "x😀", "office", 1), listOf(note.codePointCount(0, note.length), note.length, note.takeLast(3), r.extra()["vis"], r.undo))
        assertEquals("x".repeat(280), MarkSheet.noteCut("x".repeat(400))); assertEquals("as typed  ", MarkSheet.noteCut("as typed  "))
        // blanked: both keys go
        r.open(); assertTrue(r.st!!.office); r.note("   ")
        assertEquals("DONE · undo 2 · {}", r.done())
        // Office only with no note is nothing to keep, and nothing changed
        r.open(); r.office(true)
        assertEquals("UNCHANGED · undo 2 · {}", r.done())
        // a note kept as typed, its line break too; back to Client & vendors deletes `vis`
        r.open(); r.note("line one\nline two "); r.office(true); r.done()
        assertEquals(mapOf("note" to "line one\nline two ", "vis" to "office"), r.extra())
        r.open(); r.office(false)
        assertEquals("DONE · undo 4 · {note=line one\nline two }", r.done())
        // an audience this build does not know is put right by any Done (the page's own comparison)
        val odd = Run(window(""""note":"n","vis":"cv"""")).open()
        assertEquals("DONE · undo 1 · {note=n}", odd.done())
    }

    @Test fun `an Other takes a trade -- General deletes it -- and the head and the button follow the picker`() {
        val r = Run(prepOf(markJson("X", "FX1"))).open()
        assertNull(r.view().tradeBlock!!.chips, "the picker is shut")
        r.trade()
        assertEquals("FX1 · Other | trade #adb5bd General | TRADE #adb5bd General ▾ | STATE | now #6b6f68 Existing | move wish #0f766e → Client wish ~ Client wants this changed | " +
            "NOTE 0/280 [] ph[A note on this mark (optional)] Client & vendors(on) / Office only | Done on | picker Trade · FX1: Civil #b197fc, Doors & win. #4dabf7, Grills #82c91e, " +
            "Electrical #ffd43b, CCTV & data #7048e8, AC #99e9f2, False ceil. #96f2d7, Carpentry #a0522d, Plumbing #15aabf, Tiling #ff8787, Painting #63e6be, Curtains #c2255c, General #adb5bd(on)", r.shown())
        r.cat("zzz"); assertTrue(r.st!!.picker, "an id outside the 13 picks nothing")
        r.cat("carpentry"); assertEquals(listOf<Any?>("carpentry", false), listOf(r.st!!.cat, r.st!!.picker))
        assertEquals("FX1 · Other | trade #a0522d Carpentry | TRADE #a0522d Carpentry ▾ | STATE | now #6b6f68 Existing | move wish #0f766e → Client wish ~ Client wants this changed | " +
            "NOTE 0/280 [] ph[A note on this mark (optional)] Client & vendors(on) / Office only | Done on", r.shown())
        r.cat("civil"); assertEquals("carpentry", r.st!!.cat, "with the picker shut there is no chip to tap")
        assertEquals("DONE · undo 1 · {cat=carpentry}", r.done())
        r.open(); r.trade(); r.cat("general")
        assertEquals("DONE · undo 2 · {}", r.done(), "General is the default: never written")
        // a stored cat that is no trade reads General on the head and the button, and NO chip of the picker is on for it
        val z = Run(prepOf(markJson("X", "FX1", """"cat":"zzz""""))).open(); z.trade()
        assertEquals(listOf("General", "General ▾"), z.view().let { listOf(it.trade.text, it.tradeBlock!!.button.text) })
        assertTrue(z.view().tradeBlock!!.chips!!.none { it.on })
        assertEquals("UNCHANGED · undo 0 · {cat=zzz}", z.done(), "... and it is kept as it came until a trade is picked")
        // a tap on a bare part of the sheet shuts the picker
        r.open(); r.trade(); assertTrue(r.st!!.picker); r.st = MarkSheet.closePicker(r.st!!); assertFalse(r.st!!.picker); assertSame(r.st, MarkSheet.closePicker(r.st!!))
        // a type with a trade of its own has no TRADE block, and its head shows that trade whatever `cat` it carries
        val w = Run(window(""""cat":"carpentry"""")).open()
        assertNull(w.view().tradeBlock); assertEquals("Doors & win.", w.view().trade.text); w.trade(); assertFalse(w.st!!.picker)
        // a wall's S is the Switch plate, Electrical's; the floor's the Step, Civil's
        assertEquals("Electrical", Run(prepOf(markJson("S", "FS1"))).open().view().trade.text)
        val floor = SurveyPrep.fresh(3319.0, 2678.0, 2677.0).also { it.surface("floor").marks.add(SurveyPrep.Mark("a", "S", "GS1", 0.0, 1.0, 0.0, 1.0)) }
        assertEquals(listOf("GS1", "Step", "Civil"), MarkSheet.view(Tx.EN, floor, MarkSheet.open(floor, "a")!!, 1)!!.let { listOf(it.code, it.name, it.trade.text) })
    }

    @Test fun `a locked mark changes its note alone -- its moves, its acts and its trade are drawn off and none is applied`() {
        val r = Run(prepOf(markJson("D", "FD1", """"life":"wish","lifeAt":"$before","lifeAs":"client","act":"remove","locked":true"""))).open()
        assertEquals("FD1 · Door | trade #4dabf7 Doors & win. | STATE | now #0f766e Client wish · since 10 Nov, 06:37 IST · decided by the client | acts ↻ Replace(off), ⇢ Shift(off), ⚒ Repair(off), ✕ Remove(on)(off) | " +
            "move drop #b3261e → Dropped (off) + why[](off) max 140 ph[Why? — at least 3 characters] | move exist #6b6f68 ← Back to Existing (off) | foot: FD1 is locked — only its note can change | " +
            "NOTE 0/280 [] ph[A note on this mark (optional)] Client & vendors(on) / Office only | Done on", r.shown())
        val st = r.st
        r.move("exist"); r.why("drop", "because"); r.move("drop"); r.act("shift"); assertEquals(st, r.st, "no tap lands")
        r.note("ask the client")
        assertEquals("DONE · undo 1 · {life=wish, lifeAt=$before, lifeAs=client, act=remove, locked=true, note=ask the client}", r.done())
        // even a state that holds a move for it -- the screen cannot make one -- is not applied: the move is un-picked, the sheet stays
        val held = MarkSheet.open(r.prep, "a")!!.copy(to = "exist")
        var undo = 0
        val out = MarkSheet.apply(r.prep, held, 1, at) { undo++ }
        assertEquals(listOf<Any?>(Outcome.STAYS, null, 0, "wish"), listOf(out.outcome, out.state!!.to, undo, r.extra()["life"]))
        // a locked Other keeps its trade: the picker does not open
        val o = Run(prepOf(markJson("X", "FX1", """"cat":"carpentry","locked":true"""))).open()
        assertFalse(o.view().tradeBlock!!.enabled); o.trade(); assertFalse(o.st!!.picker); o.note("n")
        assertEquals("DONE · undo 1 · {cat=carpentry, locked=true, note=n}", o.done())
        // a mark in Scope is locked BY its state: its way out is offered, and its acts are on
        val s = Run(window(""""life":"scope","lifeAt":"$before","lifeAs":"staff","act":"replace","locked":true"""), PhaseTools.VENDOR_REVIEW).open()
        assertEquals(listOf(true), s.view().state!!.moves.map { it.enabled }); assertTrue(s.view().state!!.acts.all { it.enabled }); assertTrue(s.view().state!!.foots.isEmpty())
    }

    @Test fun `a Line and an area take a trade and a note, never a state -- a locked one its note alone`() {
        val r = Run(prepOf("""{"id":"a","kind":"line","code":"F-M1","pts":[[0,0],[9,0]]}""")).open()
        assertNull(r.view().state); r.move("wish"); r.act("shift"); assertEquals(listOf<Any?>(null, "replace"), listOf(r.st!!.to, r.st!!.act))
        r.trade(); r.cat("painting"); r.note("cornice"); r.office(true)
        assertEquals("F-M1 · Marked length | trade #63e6be Painting | TRADE #63e6be Painting ▾ | NOTE 7/280 [cornice] ph[A note on this length (optional)] Client & vendors / Office only(on) | Done on", r.shown())
        assertEquals("DONE · undo 1 · {cat=painting, note=cornice, vis=office}", r.done())
        val m = r.prep.surface("front").meas[0]; m.extra = SurveyPrep.withKey(m.extra, "locked", true)
        r.open(); r.trade(); assertFalse(r.st!!.picker, "locked: the picker does not open")
        r.note("cornice, both sides"); r.office(false)
        assertEquals("DONE · undo 2 · {cat=painting, note=cornice, both sides, locked=true}", r.done())
        // for every stage the sheet is the same: a Line has no state to gate
        val area = prepOf("""{"id":"a","kind":"area","code":"F-M2","pts":[[0,0],[9,0],[9,9]],"cat":"tiling","locked":true,"note":"skirting too"}""")
        assertEquals(1, STAGES.map { row(MarkSheet.view(Tx.EN, area, MarkSheet.open(area, "a")!!, it)!!) }.toSet().size)
    }

    @Test fun `Scope after the estimate -- out of scope unlocks, into scope locks, and the office is told each time`() {
        val r = Run(window(""""life":"scope","lifeAt":"$before","lifeAs":"staff","act":"replace","locked":true"""), PhaseTools.REVISIONS).open()
        assertNull(r.view().state!!.amber, "nothing picked: no amber line")
        r.move("cok")
        assertEquals("FW1 · Window | trade #4dabf7 Doors & win. | STATE | now #1d1f1a Scope · since 10 Nov, 06:37 IST · decided by staff | acts ↻ Replace(on), ⇢ Shift, ⚒ Repair, ✕ Remove | " +
            "move cok #2b8a3e ← Out of scope (on) | amb: After the estimate — tell the office | NOTE 0/280 [] ph[A note on this mark (optional)] Client & vendors(on) / Office only | Done on", r.shown())
        assertEquals("DONE · undo 1 · {life=cok, lifeAt=$now, lifeAs=staff, act=replace, locked=false} · amber", r.done())
        r.open(); r.move("scope")
        assertEquals("FW1 · Window | trade #4dabf7 Doors & win. | STATE | now #2b8a3e Client approved · since 11 Nov, 10:30 IST · decided by staff | acts ↻ Replace(on), ⇢ Shift, ⚒ Repair, ✕ Remove | " +
            "move scope #1d1f1a → Scope (on) | move wish #0f766e ← Client wish ~ client changes it | move drop #b3261e → Dropped (off) + why[] max 140 ph[Why? — at least 3 characters] | " +
            "amb: After the estimate — tell the office | NOTE 0/280 [] ph[A note on this mark (optional)] Client & vendors(on) / Office only | Done on", r.shown())
        assertEquals("DONE · undo 2 · {life=scope, lifeAt=$now, lifeAs=staff, act=replace, locked=true} · amber", r.done())
        assertEquals("After the estimate — tell the office", MarkLife.afterEstimate(Tx.EN))
        // before the estimate the same move tells nobody
        val v = Run(window(""""life":"cok""""), PhaseTools.VENDOR_REVIEW).open(); v.move("scope")
        assertNull(v.view().state!!.amber); assertEquals("DONE · undo 1 · {life=scope, lifeAt=$now, lifeAs=staff, locked=true}", v.done())
    }

    @Test fun `a Note a file holds needs its words -- Done with none says so and stays open`() {
        val r = Run(prepOf(markJson("NOTE", "FNOTE1", """"note":"old""""))).open()
        // TRADE and NOTE, no STATE. The head names the type, as the page's does: slice L5 gave the phone the Note type
        // (until then the head read its token, NOTE).
        assertEquals("FNOTE1 · Note | trade #adb5bd General | TRADE #adb5bd General ▾ | NOTE 3/280 [old] ph[What should the office or the vendor know here?] Client & vendors(on) / Office only | Done on", r.shown())
        r.note("  ")
        assertEquals("NEEDS_WORDS · undo 0 · {note=old}", r.done()); assertNotNull(r.st, "the sheet stays open"); assertEquals("  ", r.st!!.note)
        r.note("new words"); r.trade(); r.cat("civil")
        assertEquals("DONE · undo 1 · {note=new words, cat=civil}", r.done())
        assertEquals("A Note needs its words — type them, or delete the Note", MarkSheet.needsWords(Tx.EN))
    }

    @Test fun `the stage moves under an open sheet -- the move picked is no longer offered, so it is un-picked and nothing is written`() {
        val r = Run(window()).open()
        r.move("wish"); r.stage = PhaseTools.ESTIMATION
        assertEquals("STAYS · undo 0 · {}", r.done()); assertNull(r.st!!.to)
        // ... and at a gate no tap lands at all
        for (stage in listOf(PhaseTools.SURVEY, PhaseTools.APPROVED)) {
            val g = Run(window(wish), stage).open(); val st = g.st
            g.move("drop"); g.why("drop", "because"); g.act("shift"); assertEquals(st, g.st, "stage $stage")
            g.note("still his to write")
            assertEquals("DONE · undo 1 · {life=wish, lifeAt=$before, lifeAs=client, act=replace, note=still his to write}", g.done(), "the note is written at every stage")
        }
        // the item gone from under the sheet: nothing is drawn, and Done closes it
        val gone = Run(window()).open(); gone.prep.surface("front").marks.clear()
        assertNull(MarkSheet.view(Tx.EN, gone.prep, gone.st!!, 1))
        var undo = 0
        assertEquals(listOf<Any?>(Outcome.GONE, null, 0), MarkSheet.apply(gone.prep, gone.st!!, 1, at) { undo++ }.let { listOf(it.outcome, it.state, undo) })
        for (f in listOf<(MarkSheet.State) -> MarkSheet.State>({ MarkSheet.pickMove(gone.prep, it, 1, "wish") }, { MarkSheet.typeWhy(gone.prep, it, 1, "drop", "why") },
            { MarkSheet.pickAct(gone.prep, it, 1, "shift") }, { MarkSheet.togglePicker(gone.prep, it) })) assertSame(gone.st, f(gone.st!!))
    }

    // ------------------------------------------------------------------ every write through MarkLife; the file keeps unknown keys whole

    @Test fun `every write replaces the item's map and never changes it in place -- the undo copy keeps the map it had, and unknown keys come through whole, in their place`() {
        val json = markJson("W", "FW1", """"locked":false,"later":{"by":"v70","n":[1,null]},"zz":5""")
        val r = Run(prepOf(json)).open()
        val old = r.extra(); val oldText = old.toString()
        var copy: SurveyPrep.Prep? = null
        r.move("wish"); r.act("repair"); r.note("install UPVC window")
        val out = MarkSheet.apply(r.prep, r.st!!, 1, at) { copy = r.prep.copy() }
        assertEquals(Outcome.DONE, out.outcome)
        // the undo copy was taken BEFORE the first write, and shares the old map, which nothing changed
        assertSame(old, copy!!.markById("a")!!.second.extra); assertEquals(oldText, old.toString()); assertNotSame(old, r.extra())
        assertEquals("exist", MarkLife.lifeOf(copy!!.markById("a")!!.second))
        // the unknown keys are where they were, whole; the new ones after them, in the order the prototype writes them
        assertEquals(listOf("locked", "later", "zz", "note", "life", "lifeAt", "lifeAs", "act"), r.extra().keys.toList())
        assertSame(old["later"], r.extra()["later"]); assertEquals(5L, (r.extra()["zz"] as Number).toLong())
        // ... and through the prep file: saved, read back, every key of the mark as it stood
        @Suppress("UNCHECKED_CAST") fun els(tree: Map<String, Any?>) = ((tree["w"] as Map<String, Map<String, Any?>>).getValue("front")["els"] as List<Map<String, Any?>>)
        val saved = SurveyPrepFile.encode(r.prep, 3319.0, 2678.0, 2677.0)
        assertEquals(listOf("id", "t", "code", "x0", "x1", "y0", "y1", "locked", "later", "zz", "note", "life", "lifeAt", "lifeAs", "act"), els(saved)[0].keys.toList())
        val back = assertNotNull(SurveyPrepFile.decode(saved)).prep.markById("a")!!.second
        assertEquals(r.extra().toString(), back.extra.toString())
        assertEquals(listOf("wish", "repair", "install UPVC window", now, "client"), listOf(MarkLife.lifeOf(back), MarkLife.actOf(back), MarkLife.noteOf(back.extra), MarkLife.lifeAt(back), MarkLife.lifeAs(back)))
        // taken there and back, the mark is the mark that left, key for key
        val there = Run(window(""""zz":5""")).open(); val start = there.extra().toString()
        there.move("wish"); there.done(); there.open(); there.move("exist"); there.done()
        assertEquals(start, there.extra().toString()); assertEquals(2, there.undo)
        // a v52 prep with a sheet opened and shut on every item saves as it was
        val v52 = prepOf(markJson("D", "FD1", """"locked":true""")); val tree = SurveyPrepFile.encode(v52, 3319.0, 2678.0, 2677.0).toString()
        Run(v52).open().done(); assertEquals(tree, SurveyPrepFile.encode(v52, 3319.0, 2678.0, 2677.0).toString())
    }

    // ------------------------------------------------------------------ the wiring, read from the app's sources (compiled only in CI)

    private val app = File("../app/src/main/kotlin/com/malletcrafts/interiorestimate")

    /** A source file's code lines: its `//` and KDoc lines left out, so a comment that names a call is not a call. */
    private fun code(name: String): String {
        assertTrue(app.isDirectory, "the app's sources are beside :pano (${app.absolutePath})")
        return File(app, name).readLines().filter { l -> l.trimStart().let { !it.startsWith("//") && !it.startsWith("*") && !it.startsWith("/*") } }.joinToString("\n")
    }

    @Test fun `survey prep is GIVEN its project's stage, with no default, at both call sites -- by the one rule the project line uses`() {
        val prep = code("SurveyPrepScreen.kt")
        assertTrue(Regex("""\n\s+quoteStage: Int\?,\n""").containsMatchIn(prep), "SurveyPrepDialog's quoteStage parameter, with no default")
        assertFalse(Regex("""quoteStage\s*:\s*Int\?\s*=""").containsMatchIn(prep), "the stage has a default again: a caller could forget it, and every room would open under one stage's rules")
        val main = code("MainActivity.kt")
        assertEquals(2, Regex("""\bSurveyPrepDialog\(""").findAll(main).count(), "the two call sites")
        assertEquals(listOf("k.row.project, k.row.customerName, k.row.projectTitle", "pending.projectName, pending.customerName, pending.projectTitle"),
            Regex("""quoteStage = QuoteStageSync\.prepStage\(context,\s*WishSync\.stageKey\(context, ([^)]*)\)\),""").findAll(main).map { it.groupValues[1] }.toList(),
            "each hands the stage of the capture's OWN project: its ERP id, its client and its title")
        val sync = code("QuoteStageSync.kt")
        assertTrue(sync.contains("fun prepStage(context: Context, stageKey: String?): Int? {\n        val local = stageKey.isNullOrBlank()\n" +
            "        return PhaseTools.stageOf(if (local) null else cached(context, stageKey!!), local)"), "the one decision: PhaseTools.stageOf over the store's copy")
        assertTrue(code("WishSync.kt").contains("fun stageKey(context: Context, project: Catalogue.Project): String? =\n        stageKey(context, project.serverId, project.client, project.title)"),
            "one rule for a project's stage key, whether a screen holds the project or a capture names it")
    }

    @Test fun `the sheet's Done takes its ONE undo step through apply, Drop opens the sheet with none, and no screen writes a mark's life itself`() {
        val prep = code("SurveyPrepScreen.kt")
        assertEquals(1, Regex("""MarkSheet\.apply\(""").findAll(prep).count())
        assertTrue(prep.contains("val done = MarkSheet.apply(prep, st, quoteStage, java.time.Instant.now()) { snapshot() }"), "Done: apply, with the screen's snapshot as its one undo step")
        val body = prep.substringAfter("fun sheetDone() {").substringBefore("fun deleteMark(")
        assertEquals(1, Regex("""snapshot\(\)""").findAll(body).count(), "no snapshot of the screen's own beside apply's")
        assertTrue(body.contains("MarkSheet.Outcome.DONE -> { changed(); if (done.amber) say(MarkLife.afterEstimate(tx)) }"), "written: saved, and the office told of an amber move")
        // Delete on a mark past Existing: the sheet, on Dropped, and nothing else
        assertTrue(prep.contains("MarkLife.DeleteAct.DROP_SHEET -> { openSheet(mid, MarkLife.DROP); return }"))
        assertTrue(prep.contains("fun openSheet(mid: String, focusMove: String? = null, fresh: Boolean = false) { ctx = null; markSheet = MarkSheet.open(prep, mid, focusMove, fresh, hist.size) }"), "opening takes no undo step")
        // the sheet is drawn from MarkSheet.view for the stage the dialog was given, and every tap goes to a pure function
        assertTrue(prep.contains("val drawn = MarkSheet.view(tx, prep, st, quoteStage)"))
        for (f in listOf("pickSub(prep, cur, sub)", "pickMove(prep, cur, quoteStage, to)", "typeWhy(prep, cur, quoteStage, to, typed)", "pickAct(prep, cur, quoteStage, act)", "typeNote(cur, typed)",
            "pickOffice(cur, office)", "togglePicker(prep, cur)", "pickCat(cur, cat)", "closePicker(cur)")) assertTrue(prep.contains("MarkSheet.$f"), f)
        // a code is a button exactly where a sheet opens for it: the picked mark's label, a picked Line's or area's code, a list row's code
        assertEquals(3, Regex("""contentDescription = tx\.life_aria_open_mark_sheet_x\(""").findAll(prep).count())
        assertEquals(2, Regex("""if \(!MarkSheet\.opens\(prep, (?:sq\.)?id\)\) Modifier else""").findAll(prep).count(), "a Tape's and a chain's code is no button")
        // EVERY WRITE GOES THROUGH MarkSheet: no file of the app moves a mark, sets its act, its note or its trade, or touches `extra`
        val files = app.walk().filter { it.isFile && it.extension == "kt" }.map { it.name }.sorted().toList()
        val writers = Regex("""MarkLife\s*\.\s*(move|setAct|setNote|withNote|placeAs)\(|Trades\s*\.\s*(setCat|withCat)\(|\.extra\s*=[^=]""")
        assertEquals(emptyList(), files.filter { writers.containsMatchIn(code(it)) }, "a screen writes a mark's keys itself")
        assertEquals(emptyList(), files.filter { Regex("""MarkTypes\s*\.\s*(setSub|withSub)\(""").containsMatchIn(code(it)) }, "a screen writes a mark's kind itself: it is the sheet's Done")
        assertEquals(listOf("SurveyPrepScreen.kt"), files.filter { Regex("""MarkSheet\s*\.\s*(apply|open)\(""").containsMatchIn(code(it)) })
    }

    @Test fun `the sheet opens whole, with no handle, in survey prep's own colours -- and its file holds no rule`() {
        val sheet = code("MarkSheet.kt")
        assertTrue(sheet.contains("sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),"), "it opens WHOLE, as the album's sheets do")
        assertTrue(sheet.contains("onDismissRequest = a.onCancel,"), "the scrim, ◁ and a swipe cancel")
        assertTrue(sheet.contains("dragHandle = null,") && sheet.contains("containerColor = MS_BG,") && sheet.contains("sheetMaxWidth = Dp.Unspecified,"))
        assertTrue(sheet.contains(".imePadding()"), "it rises over the keyboard")
        // every text it draws is handed to it: it names no table and no life-cycle rule
        assertFalse(Regex("""\btx\b|LocalTx|MarkLife|PhaseTools|Trades""").containsMatchIn(sheet), "app/MarkSheet.kt reads a text or a rule itself")
        // the picker takes no focus and no outside tap: the three cases are the sheet's own
        assertTrue(sheet.contains("properties = PopupProperties(focusable = false, dismissOnBackPress = false, dismissOnClickOutside = false),"))
    }

    // ------------------------------------------------------------------ port 62 slice L6: KIND, and a NEW Note
    // (the page's own sheet, driven: proto-tables.mjs, "L6 journeys of the sheet")

    private fun unit(extra: String = "") = prepOf(markJson("CRP", "FCRP1", extra))

    @Test fun `L6 - KIND - a wardrobe made a Kitchen base - the head follows the chip, Done stores it in one undo step and tells the tool - and the first kind is never stored`() {
        val r = Run(unit()).open()
        assertEquals(listOf<Any?>("wardrobe", "wardrobe", false), listOf(r.st!!.sub, r.st!!.sub0, r.st!!.fresh))
        assertEquals("FCRP1 · Wardrobe | trade #a0522d Carpentry | KIND Wardrobe(on), Kitchen base, Kitchen wall unit, TV unit, Study, Shoe rack, Vanity, Other unit | STATE | now #6b6f68 Existing | " +
            "move wish #0f766e → Client wish ~ Client wants this changed | NOTE 0/280 [] ph[A note on this mark (optional)] Client & vendors(on) / Office only | Done on", r.shown())
        r.sub("kbase"); assertEquals(listOf<Any?>("kbase", "wardrobe"), listOf(r.st!!.sub, r.st!!.sub0))
        assertEquals("FCRP1 · Kitchen base | trade #a0522d Carpentry | KIND Wardrobe, Kitchen base(on), Kitchen wall unit, TV unit, Study, Shoe rack, Vanity, Other unit | STATE | now #6b6f68 Existing | " +
            "move wish #0f766e → Client wish ~ Client wants this changed | NOTE 0/280 [] ph[A note on this mark (optional)] Client & vendors(on) / Office only | Done on", r.shown())
        val old = r.extra()
        assertEquals("DONE · undo 1 · {sub=kbase}", r.done()); assertNull(r.st); assertNotSame(old, r.extra(), "the map is replaced, never changed in place"); assertTrue(old.isEmpty())
        // the tool remembers the last kind used: Done says which, for the surface the mark is on
        assertEquals(listOf("CRP", "front", "kbase"), r.last!!.kind!!.let { listOf(it.t, it.s, it.sub) })
        val mem = HashMap<String, String>(); r.last!!.kind!!.let { SurveyPrepPalette.rememberSub(mem, it.t, it.s, it.sub) }
        assertEquals(listOf<Any?>("kbase", "bed"), listOf(SurveyPrepPalette.subFor(mem, "CRP", "left"), SurveyPrepPalette.subFor(mem, "CRP", "floor")))
        // the box is as it was: a kind changed in the sheet keeps the box the mark has
        r.prep.markById("a")!!.second.let { assertEquals(listOf(0.0, 1.0, 0.0, 1.0), listOf(it.x0, it.x1, it.y0, it.y1)) }
        // reopened on the kind it is; back to the first DELETES the key, and the tool is told that too
        r.open(); assertEquals(listOf<Any?>("kbase", "kbase"), listOf(r.st!!.sub, r.st!!.sub0)); r.sub("wardrobe")
        assertEquals("DONE · undo 2 · {}", r.done()); assertEquals("wardrobe", r.last!!.kind!!.sub)
        // a kind picked and put back is nothing changed: no undo step, nothing for the tool
        val b = Run(unit(""""sub":"tv"""")).open(); b.sub("study"); b.sub("tv")
        assertEquals("UNCHANGED · undo 0 · {sub=tv}", b.done()); assertNull(b.last!!.kind)
    }

    @Test fun `L6 - KIND - a kind nobody knows reads as the first and is kept until another is picked - and an id that is no kind picks nothing`() {
        val r = Run(unit(""""sub":"zzz"""")).open()
        assertEquals(listOf<Any?>("wardrobe", "wardrobe", "Wardrobe"), listOf(r.st!!.sub, r.st!!.sub0, r.view().name))
        assertEquals("UNCHANGED · undo 0 · {sub=zzz}", r.done())
        r.open(); r.sub("wardrobe"); assertEquals("UNCHANGED · undo 0 · {sub=zzz}", r.done(), "the first is where the sheet already stood")
        r.open(); r.sub("shoe"); assertEquals("DONE · undo 1 · {sub=shoe}", r.done())
        // an id of another kind of surface, of no kind, and a type with none: the state is the very one it was
        val w = Run(unit()).open(); val st = w.st
        for (k in listOf("bed", "island", "zzz", "")) { w.sub(k); assertSame(st, w.st, k) }
        val win = Run(window()).open(); val ws = win.st; win.sub("wardrobe"); assertSame(ws, win.st); assertNull(win.view().kind); assertNull(ws!!.sub)
        val line = Run(prepOf("""{"id":"a","kind":"line","code":"F-M1","pts":[[0,0],[9,0]]}""")).open(); val ls = line.st; line.sub("wardrobe"); assertSame(ls, line.st); assertNull(line.view().kind)
        val gone = Run(unit()).open(); gone.prep.surface("front").marks.clear(); val gs = gone.st; gone.sub("kbase"); assertSame(gs, gone.st)
    }

    @Test fun `L6 - KIND - a locked mark keeps its kind, a Scope mark's too - the chips are drawn off, no tap lands, and Done writes the note alone`() {
        val r = Run(prepOf(markJson("PLB", "FPLB1", """"sub":"hot","locked":true"""))).open()
        assertEquals("KIND Cold(off), Hot(on)(off), Mixer (hot + cold)(off), Washing machine(off), RO / purifier(off), Waste outlet(off)", r.shown().split(" | ")[2])
        assertEquals("FPLB1 · Plumbing point · Hot", r.shown().split(" | ")[0])
        val st = r.st; r.sub("cold"); assertSame(st, r.st); r.note("n")
        assertEquals("DONE · undo 1 · {sub=hot, locked=true, note=n}", r.done()); assertNull(r.last!!.kind)
        // even a state that holds another kind for it -- the screen cannot make one -- is not applied
        val held = MarkSheet.open(r.prep, "a")!!.copy(sub = "cold")
        var undo = 0
        assertEquals(listOf<Any?>(Outcome.UNCHANGED, 0, "hot"), MarkSheet.apply(r.prep, held, 1, at) { undo++ }.let { listOf(it.outcome, undo, r.extra()["sub"]) })
        // a mark in Scope is locked BY its state: its moves are offered, its kind is not (the page's `lk` for KIND is the lock itself)
        val s = Run(prepOf(markJson("AC", "FAC1", """"life":"scope","lifeAs":"staff","locked":true""")), PhaseTools.VENDOR_REVIEW).open()
        assertFalse(s.view().kind!!.enabled); assertEquals(listOf(true), s.view().state!!.moves.map { it.enabled })
        val ss = s.st; s.sub("outdoor"); assertSame(ss, s.st)
        // ... and its Done writes what a Done on a mark past Existing with no act stored always wrote, and no kind (the page: act replace)
        assertEquals("DONE · undo 1 · {life=scope, lifeAs=staff, locked=true, act=replace}", s.done())
    }

    @Test fun `L6 - KIND - a Defect's trade follows its kind, a kind is written before the note and the move, and it is the mark's at every stage`() {
        val d = Run(prepOf(markJson("DEF", "FDEF1"))).open()
        assertEquals("FDEF1 · Crack | trade #63e6be Painting | KIND Crack(on), Damp / seepage, Hollow plaster, Peeling paint, Broken / hollow tile", d.shown().split(" | ").take(3).joinToString(" | "))
        d.sub("tile")
        assertEquals("FDEF1 · Broken / hollow tile | trade #ff8787 Tiling | KIND Crack, Damp / seepage, Hollow plaster, Peeling paint, Broken / hollow tile(on)", d.shown().split(" | ").take(3).joinToString(" | "))
        assertEquals("DONE · undo 1 · {sub=tile}", d.done()); assertEquals(Trades.TILING, Trades.of("front", d.prep.markById("a")!!.second))
        // the kind, then the note, then the move's keys: the prototype's order
        val s = Run(prepOf(markJson("S", "FS1"))).open()
        s.sub("meter"); s.move("wish"); s.act("shift"); s.note("move it to the passage")
        assertEquals("FS1 · Meter board | trade #ffd43b Electrical | KIND Switch plate, DB / MCB board, Meter board(on) | STATE | now #6b6f68 Existing | acts ↻ Replace, ⇢ Shift(on), ⚒ Repair, ✕ Remove | " +
            "move wish #0f766e → Client wish ~ Client wants this changed (on) | NOTE 22/280 [move it to the passage] ph[A note on this mark (optional)] Client & vendors(on) / Office only | Done on", s.shown())
        assertEquals("DONE · undo 1 · {sub=meter, note=move it to the passage, life=wish, lifeAt=$now, lifeAs=client, act=shift}", s.done())
        assertEquals(listOf("sub", "note", "life", "lifeAt", "lifeAs", "act"), s.extra().keys.toList())
        // Survey's gate and the approval lock hold the MOVES; the kind is what the thing is, at every stage and unread
        for (stage in STAGES) { val r = Run(unit(), stage).open(); assertTrue(r.view().kind!!.enabled, "stage $stage"); r.sub("vanity"); assertEquals("DONE · undo 1 · {sub=vanity}", r.done(), "stage $stage") }
        // a kind beside unknown keys: they stay where they were, whole, and `sub` lands after them
        val k = Run(unit(""""locked":false,"later":{"by":"v70"},"zz":5""")).open(); k.sub("tv"); k.done()
        assertEquals(listOf("locked", "later", "zz", "sub"), k.extra().keys.toList())
        // ... and through the prep file: the kind comes back
        val back = assertNotNull(SurveyPrepFile.decode(SurveyPrepFile.encode(k.prep, 3319.0, 2678.0, 2677.0))).prep.markById("a")!!.second
        assertEquals("TV unit", MarkTypes.displayName(Tx.EN, "front", back))
    }

    @Test fun `L6 - KIND on a floor and on the ceiling - that surface's own kinds - and none where the type has none`() {
        fun on(s: String, t: String, code: String, extra: Map<String, Any?> = emptyMap()): SurveyPrep.Prep =
            SurveyPrep.fresh(3319.0, 2678.0, 2677.0).also { it.surface(s).marks.add(SurveyPrep.Mark("a", t, code, 0.0, 1.0, 0.0, 1.0, extra)) }
        fun shown(p: SurveyPrep.Prep, tx: Tx = Tx.EN) = row(assertNotNull(MarkSheet.view(tx, p, MarkSheet.open(p, "a")!!, 1))).split(" | ").take(3).joinToString(" | ")
        assertEquals("CCDP1 · Full false ceiling | trade #96f2d7 False ceil. | KIND Bulkhead, Cove, Full false ceiling(on)", shown(on("ceil", "CDP", "CCDP1", mapOf("sub" to "full"))))
        assertEquals("GCRP1 · Bed | trade #a0522d Carpentry | KIND Bed(on), Kitchen platform, Island, Other unit", shown(on("floor", "CRP", "GCRP1")))
        assertEquals("GPLB1 · Plumbing point · Floor trap | trade #15aabf Plumbing | KIND Floor trap(on), Waste outlet, Cold, Hot", shown(on("floor", "PLB", "GPLB1")))
        // the wall's `cold` is the floor's third kind: read against the surface the mark is ON
        assertEquals("GPLB1 · Plumbing point · Cold | trade #15aabf Plumbing | KIND Floor trap, Waste outlet, Cold(on), Hot", shown(on("floor", "PLB", "GPLB1", mapOf("sub" to "cold"))))
        assertEquals("CLV1 · Camera | trade #7048e8 CCTV & data | KIND Camera(on), Wi-Fi access point, Smoke detector", shown(on("ceil", "LV", "CLV1")))
        // the floor's S is a Step: no kinds, no KIND
        val step = on("floor", "S", "GS1"); assertNull(MarkSheet.view(Tx.EN, step, MarkSheet.open(step, "a")!!, 1)!!.kind); assertNull(MarkSheet.open(step, "a")!!.sub)
        // मराठी: KIND is its own key, life.kind -- the page, which has no key for the word, shows KIND there
        assertEquals("FCRP1 · वॉर्डरोब | trade #a0522d सुतारकाम | प्रकार वॉर्डरोब(on), किचन खालचे युनिट, किचन वरचे युनिट, TV युनिट, स्टडी, शू रॅक, व्हॅनिटी, इतर युनिट", shown(unit(), Tx.MR))
        assertEquals("CCDP1 · पूर्ण फॉल्स सीलिंग | trade #96f2d7 फॉ. सीलिंग | प्रकार बल्कहेड, कोव्ह, पूर्ण फॉल्स सीलिंग(on)", shown(on("ceil", "CDP", "CCDP1", mapOf("sub" to "full")), Tx.MR))
        assertEquals("KIND", Tx.EN.life_kind)
        // every word of KIND is a cell of the tables: its label and each chip
        val p = probe(); val cell = Regex("^⟦\\d+⟧$"); val v = assertNotNull(MarkSheet.view(p, unit(), MarkSheet.open(unit(), "a")!!, 1))
        assertTrue(cell.matches(v.kind!!.label), v.kind!!.label); for (c in v.kind!!.chips) assertTrue(cell.matches(c.text), c.text)
        assertEquals(9, (v.kind!!.chips.map { it.text } + v.kind!!.label + v.name).toSet().size, "eight kinds and the label: nine cells (the head's name is the first chip's)")
    }

    /** A Note as a tap places it (PhaseTools.place at the Wish list), and the screen's one undo step for it. */
    private inner class NewNote(stage: Int? = PhaseTools.WISH_LIST) {
        val prep = SurveyPrep.fresh(3319.0, 2678.0, 2677.0)
        var undo = 0
        val mark = assertNotNull(PhaseTools.place(prep, "front", MarkLife.NOTE_TYPE, 900.0, 1200.0, 2678.0, 2677.0, "a", stage, null, at) { undo++ }).mark
        var st: MarkSheet.State? = MarkSheet.open(prep, "a", fresh = true, hist = undo)
        fun there() = prep.surface("front").marks.map { it.code + " " + it.extra }
    }

    @Test fun `L6 - a NEW Note - its sheet opens on the note field, and Done with its words takes no undo step of its own`() {
        val n = NewNote()
        // placed: a pin where the finger was, 50 mm, FNOTE1, with no key at all -- a Note is never born a wish, whatever the phase
        assertEquals(listOf("FNOTE1 {}"), n.there()); assertEquals(1, n.undo)
        assertEquals(listOf(875.0, 925.0, 1175.0, 1225.0), n.mark.let { listOf(it.x0, it.x1, it.y0, it.y1) })
        assertEquals(MarkSheet.State("a", null, null, emptyMap(), "replace", null, "", false, "general", "general", false, null, null, true, 1), n.st)
        val v = assertNotNull(MarkSheet.view(Tx.EN, n.prep, n.st!!, 1))
        assertTrue(v.focusNote, "the keyboard goes to the note"); assertNull(v.focusWhy); assertNull(v.state); assertNull(v.kind)
        assertEquals("FNOTE1 · Note | trade #adb5bd General | TRADE #adb5bd General ▾ | NOTE 0/280 [] ph[What should the office or the vendor know here?] Client & vendors(on) / Office only | Done on", row(v))
        // a sheet opened by a tap on a code does not take the keyboard
        assertFalse(MarkSheet.view(Tx.EN, n.prep, MarkSheet.open(n.prep, "a")!!, 1)!!.focusNote)
        n.st = MarkSheet.pickCat(MarkSheet.togglePicker(n.prep, MarkSheet.pickOffice(MarkSheet.typeNote(n.st!!, "ask about the beam"), true)), "civil")
        val done = MarkSheet.apply(n.prep, n.st!!, 1, at) { n.undo++ }
        assertEquals(listOf<Any?>(Outcome.DONE, null, 1), listOf(done.outcome, done.state, n.undo), "ONE undo step for a new Note: its placement's")
        assertEquals(listOf("FNOTE1 {cat=civil, note=ask about the beam, vis=office}"), n.there())
        // at every stage that has a palette, and unread: the same Note, with no state
        for (stage in listOf(0, 1, 2, 4, null)) assertEquals(listOf("FNOTE1 {}"), NewNote(stage).there(), "stage $stage")
    }

    @Test fun `L6 - a NEW Note with no words goes again - on Done and on cancel - and its placement's undo step goes with it`() {
        // Done with nothing typed, and with only spaces
        for (typed in listOf("", "   ", "\n \n")) {
            val n = NewNote(); n.st = MarkSheet.typeNote(n.st!!, typed)
            val done = MarkSheet.apply(n.prep, n.st!!, 1, at) { n.undo++ }
            assertEquals(listOf<Any?>(Outcome.REMOVED, null, 1), listOf(done.outcome, done.state, n.undo), "'$typed': removed, and Done took no step")
            assertEquals(emptyList(), n.there()); assertTrue(MarkSheet.takesBackUndo(n.st!!, n.undo), "the placement's step is the last: it is taken back")
        }
        // ✕, the scrim, ◁: removed the same way -- even with words typed, for nothing was stored
        for (typed in listOf("", "typed, then closed")) {
            val n = NewNote(); n.st = MarkSheet.typeNote(n.st!!, typed)
            assertTrue(MarkSheet.cancel(n.prep, n.st!!), "'$typed'"); assertEquals(emptyList(), n.there()); assertTrue(MarkSheet.takesBackUndo(n.st!!, 1))
        }
        assertEquals("Empty note removed", MarkSheet.emptyRemoved(Tx.EN)); assertEquals("रिकामी नोंद काढली", MarkSheet.emptyRemoved(Tx.MR))
        // the undo step is taken back ONLY while it is still the last one, and only for a fresh sheet
        val n = NewNote(); val st = n.st!!
        assertEquals(listOf(true, false, false, false), listOf(1, 2, 0, 40).map { MarkSheet.takesBackUndo(st, it) }, "one step held as it opened: 1 matches; another taken since, none, 40: not")
        assertFalse(MarkSheet.takesBackUndo(st.copy(fresh = false), 1)); assertTrue(MarkSheet.takesBackUndo(st.copy(hist = 40), 40), "a full stack of 40 that still ends in the placement")
        assertFalse(MarkSheet.takesBackUndo(st.copy(hist = 0), 0), "nothing held: nothing to take back")
        // THE PHONE'S OWN RULE FOR CODES (v52 slice 8): a code is never handed out twice, so the next Note there is FNOTE2
        // (the page counts the marks still there and says FNOTE1 again)
        assertTrue(MarkSheet.cancel(n.prep, st))
        assertEquals("FNOTE2", assertNotNull(PhaseTools.place(n.prep, "front", MarkLife.NOTE_TYPE, 500.0, 500.0, 2678.0, 2677.0, "b", 1, null, at) { }).mark.code)
    }

    @Test fun `L6 - only a NEW and still empty Note is removed - a Note a file holds, a fresh one that has its words, a mark that is no Note, all stay`() {
        // a Note a file holds: cancel removes nothing; Done with its words blanked says so and stays open
        val r = Run(prepOf(markJson("NOTE", "FNOTE1", """"note":"old""""))).open()
        r.note(""); assertFalse(MarkSheet.cancel(r.prep, r.st!!)); assertEquals(1, r.prep.surface("front").marks.size)
        assertEquals("NEEDS_WORDS · undo 0 · {note=old}", r.done()); assertNotNull(r.st)
        // ... nor a Note a file holds with NO words (another writer's): opened by a tap on it, its sheet is not a new
        // Note's, so ✕ leaves it where it is -- only `fresh` makes cancel a removal (the page's `if(fresh&&…)`)
        val bare = prepOf(markJson("NOTE", "FNOTE1"))
        assertFalse(MarkSheet.cancel(bare, MarkSheet.open(bare, "a")!!)); assertEquals(1, bare.surface("front").marks.size)
        // a fresh sheet on a Note that already has its words: cancel keeps it; Done with them blanked removes it, as the page
        // does (it is fresh, and it would be left with no words)
        val kept = prepOf(markJson("NOTE", "FNOTE1", """"note":"kept""""))
        val fresh = MarkSheet.open(kept, "a", fresh = true, hist = 1)!!
        assertFalse(MarkSheet.cancel(kept, fresh)); assertEquals(1, kept.surface("front").marks.size)
        assertEquals(Outcome.REMOVED, MarkSheet.apply(kept, MarkSheet.typeNote(fresh, " "), 1, at) { }.outcome); assertEquals(0, kept.surface("front").marks.size)
        // a fresh flag on anything but a Note changes nothing: cancel keeps it, and Done takes no step of its own for a change
        val w = window(); val wf = MarkSheet.open(w, "a", fresh = true, hist = 3)!!
        assertFalse(MarkSheet.cancel(w, wf)); assertEquals(1, w.surface("front").marks.size)
        // a LOCKED empty Note (no tap makes one) is not taken away: Done says it needs its words
        val locked = prepOf(markJson("NOTE", "FNOTE1", """"locked":true"""))
        var undo = 0
        val out = MarkSheet.apply(locked, MarkSheet.open(locked, "a", fresh = true, hist = 1)!!, 1, at) { undo++ }
        assertEquals(listOf<Any?>(Outcome.NEEDS_WORDS, 0, 1), listOf(out.outcome, undo, locked.surface("front").marks.size))
        assertFalse(MarkSheet.cancel(locked, MarkSheet.open(locked, "a", fresh = true, hist = 1)!!)); assertEquals(1, locked.surface("front").marks.size)
        // the item gone from under a fresh sheet: nothing to remove
        val gone = NewNote(); gone.prep.surface("front").marks.clear(); assertFalse(MarkSheet.cancel(gone.prep, gone.st!!))
    }

    @Test fun `L6 - the screen removes an empty new Note as the sheet says, opens a new Note's sheet from both placements, and hands a kind to the tool`() {
        val prep = code("SurveyPrepScreen.kt")
        // a new Note's sheet opens at once, fresh, from BOTH placements -- and the two functions stand above them (a local
        // function is visible only below its declaration: the app is compiled only by CI)
        assertTrue(prep.contains("fun notePlaced(e: SurveyPrep.Mark) { if (MarkLife.isNote(e)) openSheet(e.id, fresh = true) }"))
        assertEquals(2, Regex("""\n\s+changed\(\)\n\s+notePlaced\(e\)\n\s+return e\n""").findAll(prep).count(), "2 · Mark's placement and the 360's")
        val order = listOf("var markSheet by remember", "fun openSheet(", "fun notePlaced(", "fun placeMark(", "fun place360(", "fun noteRemoved(", "fun sheetDone(", "fun sheetCancel(", "fun deleteMark(").map { prep.indexOf(it) }
        assertTrue(order.all { it > 0 } && order == order.sorted(), "declared in the order they are used: $order")
        // Done: a kind changed goes to the tool's memory; an empty new Note is removed
        val body = prep.substringAfter("fun sheetDone() {").substringBefore("fun sheetCancel(")
        assertTrue(body.contains("done.kind?.let { k -> SurveyPrepPalette.rememberSub(spKinds, k.t, k.s, k.sub) }"))
        assertTrue(body.contains("MarkSheet.Outcome.REMOVED -> noteRemoved(st)"))
        // cancel -- ✕, the scrim, ◁, a swipe -- goes through the sheet's own rule
        assertTrue(prep.contains("fun sheetCancel() {\n        val st = markSheet ?: return\n        markSheet = null\n        if (MarkSheet.cancel(prep, st)) noteRemoved(st)\n    }"))
        assertTrue(prep.contains("onCancel = { sheetCancel() }))")); assertEquals(0, Regex("""onCancel = \{ markSheet = null \}""").findAll(prep).count())
        assertTrue(prep.contains("onKind = { sub -> markSheet = markSheet?.let { cur -> MarkSheet.pickSub(prep, cur, sub) } },"))
        // removed: the placement's undo step taken back while it is the last, the pick let go, saved, and said with no UNDO
        assertTrue(prep.contains("fun noteRemoved(st: MarkSheet.State) {\n        if (MarkSheet.takesBackUndo(st, hist.size)) hist.removeAt(hist.size - 1)\n" +
            "        if (sel?.id == st.id) { sel = null; active = null }\n        changed()\n        snack(MarkSheet.emptyRemoved(tx), undo = false)\n    }"))
        // the sheet draws KIND first, in both orientations, and a new Note's field takes the keyboard
        val sheet = code("MarkSheet.kt")
        assertEquals(2, Regex("""val kind = v\.kind\n\s+if \(kind != null\) MsKind\(kind, a\.onKind\)""").findAll(sheet).count())
        assertEquals(2, Regex("""MsNote\(v\.note, v\.focusNote, a\)""").findAll(sheet).count())
        assertTrue(sheet.contains("LaunchedEffect(Unit) { if (focus) runCatching { noteFocus.requestFocus() } }"))
        assertTrue(sheet.contains("for (c in k.chips) MsChip(c.text, null, c.on, k.enabled, Modifier) { onKind(c.id) }"))
    }
}
