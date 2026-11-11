package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.MarkLife.DeleteAct
import com.malletcrafts.interiorestimate.pano.MarkLife.DeleteWord
import com.malletcrafts.interiorestimate.pano.MarkLife.Refusal
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * A mark's life cycle (port 62 slice L1), against the approved prototype v61c.2 (index61c2.html, L53: LIFE 2756-2764,
 * STROKE 2768, ACTS 2770, spared 2789, MOVES 2794-2811, allowedMoves 2818, move 2834-2846, asPlace 2848, the delete,
 * unlock and clear rules 2936-2955, the counts 2964-2968): every row of the table, what each mark is offered at each
 * of the six stages and while unread, and every key a move writes, in the order it writes it. The expected values are
 * written out by hand; the rows of the offered moves and the worked journeys are what the prototype's own module
 * answers for the same input -- except at Client approved, where the phone offers no move (DESIGN-port62 Q-list 6).
 */
class MarkLifeTest {

    /** 11 Nov 2026, 10:30:00 IST. */
    private val at: Instant = Instant.parse("2026-11-11T05:00:00Z")
    private val stamp = "2026-11-11T10:30:00+05:30"

    private fun mark(extra: Map<String, Any?> = emptyMap(), t: String = "W", code: String = "FW1") = SurveyPrep.Mark("a", t, code, 0.0, 1.0, 0.0, 1.0, extra)

    /** A mark in state [life] that was born [born], with the keys in the order the prototype writes them. */
    private fun mk(life: String, born: String = "exist", t: String = "W"): SurveyPrep.Mark {
        val x = LinkedHashMap<String, Any?>()
        if (born != "exist") x["born"] = born
        if (life != "exist") x["life"] = life
        return mark(x, t)
    }

    private fun keys(e: SurveyPrep.Mark) = e.extra.keys.toList()
    private val STAGES: List<Int?> = listOf(0, 1, 2, 3, 4, 5, null)

    // ------------------------------------------------------------------ the tables

    @Test fun `the eight states, in the life cycle's order, each with its id, its pill colour and its name in both languages`() {
        assertEquals(listOf("exist", "wish", "vrev", "vok", "vadd", "cok", "scope", "drop"), MarkLife.IDS)
        assertEquals(listOf("wish", "vrev", "vok", "vadd", "cok", "scope", "drop"), MarkLife.STORED, "Existing is never stored")
        assertEquals(listOf(0x6b6f68, 0x0F766E, 0x9a6700, 0x1864ab, 0x6741d9, 0x2b8a3e, 0x1d1f1a, 0xb3261e), MarkLife.STATES.map { it.colour })
        assertEquals(listOf("Existing", "Client wish", "Vendor review", "Vendor approved", "Vendor added", "Client approved", "Scope", "Dropped"),
            MarkLife.IDS.map { MarkLife.name(Tx.EN, it) })
        assertEquals(listOf("सध्याचे", "ग्राहकाची इच्छा", "व्हेंडर तपासणी", "व्हेंडर मंजूर", "व्हेंडरने जोडले", "ग्राहक मंजूर", "स्कोप", "वगळले"),
            MarkLife.IDS.map { MarkLife.name(Tx.MR, it) })
        // an id that is no state is Existing, in its colour, its outline and its name
        for (bad in listOf(null, "", "zzz", "Wish")) {
            assertEquals("exist", MarkLife.byId(bad).id, "$bad"); assertEquals("Existing", MarkLife.name(Tx.EN, bad)); assertNull(MarkLife.stroke(bad))
        }
        assertEquals(listOf(MarkLife.EXIST, MarkLife.WISH, MarkLife.VREV, MarkLife.VOK, MarkLife.VADD, MarkLife.COK, MarkLife.SCOPE, MarkLife.DROP), MarkLife.IDS)
    }

    @Test fun `STROKE - each state's outline, row by row -- Existing has none, it is v52's`() {
        fun show(s: MarkLife.Stroke?) = if (s == null) "v52" else listOfNotNull(
            if (s.dash.isEmpty()) "solid" else s.dash.joinToString("/") { it.toInt().toString() }, "w=${s.width}",
            if (s.round) "round" else null, if (s.alpha != 1.0) "alpha=${s.alpha}" else null, s.grey?.let { "grey=" + it.toString(16) }).joinToString(" ")
        assertEquals(listOf(
            "exist: v52",                               // solid 1.5 dp, as v52
            "wish: 6/4 w=null",                         // dashed 6/4 dp, v52's own width
            "vrev: 2/3 w=null",                         // dashed 2/3 dp
            "vok: solid w=1.5",                         // solid 1.5 dp
            "vadd: 2/3 w=null round",                   // dotted 2/3 dp: round caps
            "cok: solid w=2.5",                         // solid 2.5 dp
            "scope: solid w=2.5",                       // solid 2.5 dp, padlock
            "drop: solid w=1.0 alpha=0.4 grey=8a8f86",  // 1 dp grey, red diagonal, 40 %
        ), MarkLife.IDS.map { "$it: " + show(MarkLife.stroke(it)) })
        for (s in MarkLife.STATES) assertSame(s.stroke, MarkLife.stroke(s.id))
    }

    @Test fun `ACTS - the four acts, their glyphs and their names in both languages`() {
        assertEquals(listOf("replace", "shift", "repair", "remove"), MarkLife.ACTS.map { it.id })
        assertEquals(listOf("↻", "⇢", "⚒", "✕"), MarkLife.ACTS.map { it.glyph })
        assertEquals(listOf("Replace", "Shift", "Repair", "Remove"), MarkLife.ACTS.map { MarkLife.actName(Tx.EN, it.id) })
        assertEquals(listOf("बदला", "हलवा", "दुरुस्ती", "काढा"), MarkLife.ACTS.map { MarkLife.actName(Tx.MR, it.id) })
        assertEquals("⇢", MarkLife.glyph("shift")); assertNull(MarkLife.glyph("paint")); assertNull(MarkLife.glyph(null))
        assertEquals(listOf("client", "vendor", "staff"), listOf(MarkLife.AS_CLIENT, MarkLife.AS_VENDOR, MarkLife.AS_STAFF))
        assertEquals(listOf(280, 140, 3), listOf(MarkLife.NOTE_MAX, MarkLife.WHY_MAX, MarkLife.WHY_MIN))
    }

    @Test fun `the ten keys v53 adds to a mark, by the names the prototype stores them under`() {
        assertEquals(listOf("life", "lifeAt", "lifeAs", "why", "born", "act", "note", "vis", "sub", "cat"), MarkLife.KEYS)
        assertEquals("NOTE", MarkLife.NOTE_TYPE); assertEquals("office", MarkLife.VIS_OFFICE)
        // none of them is a key the prep file reads as a field: each lives in a mark's extra, kept whole by SurveyPrepFile
        assertEquals(emptySet(), MarkLife.KEYS.toSet() intersect SurveyPrepFile.MARK_KEYS)
    }

    @Test fun `MOVES - every row of the table - from, to, who decided, and its flags`() {
        fun row(m: MarkLife.Move) = listOfNotNull("${m.from}>${m.to}", m.asWho, if (m.why) "why" else null, if (m.back) "back" else null,
            m.bornOnly?.let { "bornOnly=$it" }, if (m.restore) "restore" else null, if (m.act) "act" else null, if (m.amber) "amber" else null).joinToString(" ")
        assertEquals(listOf(
            "exist>wish client act",
            "wish>vrev staff",
            "wish>drop client why",
            "wish>exist staff back bornOnly=exist",
            "vrev>vok vendor",
            "vrev>wish vendor why back",
            "vrev>drop vendor why",
            "vok>cok client",
            "vok>wish client back",
            "vok>drop client why",
            "vadd>cok client",
            "vadd>drop client why",
            "cok>scope staff",
            "cok>wish client back",
            "cok>drop client why",
            "scope>cok staff back",
            "drop>wish staff restore",
        ), MarkLife.MOVE_TABLE.map(::row))
        assertEquals(17, MarkLife.MOVE_TABLE.size)
        // by the state a mark is in, every state has a way on, and each row sits under its own `from`
        assertEquals(MarkLife.IDS, MarkLife.MOVES.keys.toList())
        assertEquals(listOf(1, 3, 3, 3, 2, 3, 1, 1), MarkLife.IDS.map { MarkLife.MOVES.getValue(it).size })
        for ((from, rows) in MarkLife.MOVES) for (m in rows) assertEquals(from, m.from)
    }

    @Test fun `MOVES - every row's words and hint, in both languages`() {
        assertEquals(listOf(
            "→ Client wish | Client wants this changed",
            "→ Vendor review | null",
            "→ Dropped | null",
            "← Back to Existing | null",
            "→ Vendor approved | null",
            "← Client wish | vendor asks for a change",
            "→ Dropped | vendor cannot do it",
            "→ Client approved | null",
            "← Client wish | client changes it",
            "→ Dropped | null",
            "→ Client approved | null",
            "→ Dropped | null",
            "→ Scope | null",
            "← Client wish | client changes it",
            "→ Dropped | null",
            "← Out of scope | null",
            "↺ Restore | null",
        ), MarkLife.MOVE_TABLE.map { "${MarkLife.label(Tx.EN, it)} | ${MarkLife.hint(Tx.EN, it)}" })
        assertEquals(listOf(
            "→ ग्राहकाची इच्छा | ग्राहकाला यात बदल हवा आहे",
            "→ व्हेंडर तपासणी | null",
            "→ वगळले | null",
            "← परत \"सध्याचे\" कडे | null",
            "→ व्हेंडर मंजूर | null",
            "← ग्राहकाची इच्छा | व्हेंडरला बदल हवा",
            "→ वगळले | व्हेंडरला हे जमत नाही",
            "→ ग्राहक मंजूर | null",
            "← ग्राहकाची इच्छा | ग्राहकाला बदल हवा",
            "→ वगळले | null",
            "→ ग्राहक मंजूर | null",
            "→ वगळले | null",
            "→ स्कोप | null",
            "← ग्राहकाची इच्छा | ग्राहकाला बदल हवा",
            "→ वगळले | null",
            "← स्कोपबाहेर | null",
            "↺ परत आणा | null",
        ), MarkLife.MOVE_TABLE.map { "${MarkLife.label(Tx.MR, it)} | ${MarkLife.hint(Tx.MR, it)}" })
    }

    // ------------------------------------------------------------------ reading a mark

    @Test fun `a mark with no life is Existing -- and so is one whose life is unknown, which is read, never rewritten`() {
        assertEquals("exist", MarkLife.lifeOf(mark())); assertFalse(MarkLife.pastExisting(mark()))
        for (l in MarkLife.STORED) { assertEquals(l, MarkLife.lifeOf(mark(mapOf("life" to l)))); assertTrue(MarkLife.pastExisting(mark(mapOf("life" to l)))) }
        // a newer build's state, a typo, the default written out, a value that is no string
        for (bad in listOf<Any?>("zzz", "Wish", "exist", "", 3, true, null, listOf("wish"))) assertEquals("exist", MarkLife.lifeOf(mark(mapOf("life" to bad))), "$bad")
        // a Note has no state, whatever its keys say
        assertEquals("exist", MarkLife.lifeOf(mark(mapOf("life" to "wish"), t = "NOTE")))
        assertTrue(MarkLife.isNote(mark(t = "NOTE"))); assertFalse(MarkLife.isNote(mark(t = "N"))); assertFalse(MarkLife.isNote(mark(t = "note")))
    }

    @Test fun `born - a mark was placed as a Client wish, as Vendor added, or it was there`() {
        assertEquals("exist", MarkLife.bornOf(mark()))
        assertEquals("wish", MarkLife.bornOf(mark(mapOf("born" to "wish")))); assertEquals("vadd", MarkLife.bornOf(mark(mapOf("born" to "vadd"))))
        for (bad in listOf<Any?>("exist", "cok", "drop", "", 1, null)) assertEquals("exist", MarkLife.bornOf(mark(mapOf("born" to bad))), "$bad")
        // born is what it was PLACED as and never follows the state
        assertEquals("wish", MarkLife.bornOf(mk("scope", "wish"))); assertEquals("vadd", MarkLife.bornOf(mk("drop", "vadd")))
    }

    @Test fun `the act, the why, the stamp, who decided, the note and its audience are read as stored`() {
        val e = mark(linkedMapOf("life" to "drop", "lifeAt" to stamp, "lifeAs" to "client", "act" to "shift", "why" to "no budget", "note" to "near the door", "vis" to "office"))
        assertEquals("shift", MarkLife.actOf(e)); assertEquals("no budget", MarkLife.whyOf(e)); assertEquals(stamp, MarkLife.lifeAt(e)); assertEquals("client", MarkLife.lifeAs(e))
        assertEquals("near the door", MarkLife.noteOf(e.extra)); assertTrue(MarkLife.isOffice(e.extra))
        val bare = mark()
        assertNull(MarkLife.actOf(bare)); assertEquals("", MarkLife.whyOf(bare)); assertNull(MarkLife.lifeAt(bare)); assertNull(MarkLife.lifeAs(bare))
        assertEquals("", MarkLife.noteOf(bare.extra)); assertFalse(MarkLife.isOffice(bare.extra))
        // an act that is not one of the four is none; anything but `office` is Client & vendors
        assertNull(MarkLife.actOf(mark(mapOf("act" to "paint")))); assertNull(MarkLife.actOf(mark(mapOf("act" to 2))))
        for (v in listOf<Any?>("cv", "Office", "", true, null)) assertFalse(MarkLife.isOffice(mapOf("vis" to v)), "$v")
        assertEquals("", MarkLife.noteOf(mapOf("note" to 12)))
    }

    // ------------------------------------------------------------------ what a mark is offered

    @Test fun `allowedMoves - every state, born each way, at each of the six stages and unread`() {
        // one row per (born, state): what is offered at Survey | Wish list | Vendor quoted | Estimated | Revisions | Client approved | unread.
        // `?` needs a why, `!` is told to the office (after the estimate, into or out of Scope). Client approved: none on the phone
        val want = listOf(
            Triple("exist", "exist", "- | wish | - | - | - | - | wish"),
            Triple("exist", "wish", "- | drop?,exist | vrev,drop? | - | vrev,drop? | - | vrev,drop?,exist"),
            Triple("exist", "vrev", "- | - | vok,wish?,drop? | - | vok,wish?,drop? | - | vok,wish?,drop?"),
            Triple("exist", "vok", "- | - | cok,wish,drop? | - | cok,wish,drop? | - | cok,wish,drop?"),
            Triple("exist", "vadd", "- | - | cok,drop? | - | cok,drop? | - | cok,drop?"),
            Triple("exist", "cok", "- | - | scope,wish,drop? | - | scope!,wish,drop? | - | scope,wish,drop?"),
            Triple("exist", "scope", "- | - | cok | - | cok! | - | cok"),
            Triple("exist", "drop", "- | wish | wish | - | wish | - | wish"),
            // born a Client wish: never offered ← Back to Existing -- it was never there
            Triple("wish", "exist", "- | wish | - | - | - | - | wish"),
            Triple("wish", "wish", "- | drop? | vrev,drop? | - | vrev,drop? | - | vrev,drop?"),
            Triple("wish", "vrev", "- | - | vok,wish?,drop? | - | vok,wish?,drop? | - | vok,wish?,drop?"),
            Triple("wish", "vok", "- | - | cok,wish,drop? | - | cok,wish,drop? | - | cok,wish,drop?"),
            Triple("wish", "vadd", "- | - | cok,drop? | - | cok,drop? | - | cok,drop?"),
            Triple("wish", "cok", "- | - | scope,wish,drop? | - | scope!,wish,drop? | - | scope,wish,drop?"),
            Triple("wish", "scope", "- | - | cok | - | cok! | - | cok"),
            Triple("wish", "drop", "- | wish | wish | - | wish | - | wish"),
            // born Vendor added: from Client approved it goes back to Vendor added, and a Dropped one is restored to it
            Triple("vadd", "exist", "- | wish | - | - | - | - | wish"),
            Triple("vadd", "wish", "- | drop? | vrev,drop? | - | vrev,drop? | - | vrev,drop?"),
            Triple("vadd", "vrev", "- | - | vok,wish?,drop? | - | vok,wish?,drop? | - | vok,wish?,drop?"),
            Triple("vadd", "vok", "- | - | cok,wish,drop? | - | cok,wish,drop? | - | cok,wish,drop?"),
            Triple("vadd", "vadd", "- | - | cok,drop? | - | cok,drop? | - | cok,drop?"),
            Triple("vadd", "cok", "- | - | scope,vadd,drop? | - | scope!,vadd,drop? | - | scope,vadd,drop?"),
            Triple("vadd", "scope", "- | - | cok | - | cok! | - | cok"),
            Triple("vadd", "drop", "- | vadd | vadd | - | vadd | - | vadd"),
        )
        assertEquals(24, want.map { it.first to it.second }.toSet().size)
        for ((born, life, row) in want) {
            val got = STAGES.joinToString(" | ") { st ->
                MarkLife.allowedMoves(mk(life, born), st).joinToString(",") { it.to + (if (it.amber) "!" else "") + (if (it.why) "?" else "") }.ifEmpty { "-" }
            }
            assertEquals(row, got, "born $born, in $life")
        }
    }

    @Test fun `no move at Survey, Estimated or Client approved, for any mark -- and none for a Note at any stage`() {
        for (born in listOf("exist", "wish", "vadd")) for (life in MarkLife.IDS) for (st in listOf(PhaseTools.SURVEY, PhaseTools.ESTIMATION, PhaseTools.APPROVED))
            assertEquals(emptyList(), MarkLife.allowedMoves(mk(life, born), st), "born $born, in $life, stage $st")
        for (st in STAGES) assertEquals(emptyList(), MarkLife.allowedMoves(mk("wish", t = "NOTE"), st), "stage $st")
        // a stage that is no stage is unread: every move
        assertEquals(listOf("vrev", "drop", "exist"), MarkLife.allowedMoves(mk("wish"), 9).map { it.to })
        assertEquals(listOf("vrev", "drop", "exist"), MarkLife.allowedMoves(mk("wish"), -1).map { it.to })
    }

    @Test fun `a move is resolved for its mark - Restore goes to what it was born, and a Vendor added mark goes back to Vendor added`() {
        fun offered(e: SurveyPrep.Mark, tx: Tx = Tx.EN) = MarkLife.allowedMoves(e, null).map { "${MarkLife.label(tx, it)} (${MarkLife.hint(tx, it)}) [${it.asWho}] > ${it.to}" }
        assertEquals(listOf("↺ Restore (to Client wish) [staff] > wish"), offered(mk("drop")))
        assertEquals(listOf("↺ Restore (to Client wish) [staff] > wish"), offered(mk("drop", "wish")))
        assertEquals(listOf("↺ Restore (to Vendor added) [staff] > vadd"), offered(mk("drop", "vadd")))
        assertEquals(listOf("↺ परत आणा (ग्राहकाची इच्छा कडे) [staff] > wish"), offered(mk("drop"), Tx.MR))
        assertEquals(listOf("↺ परत आणा (व्हेंडरने जोडले कडे) [staff] > vadd"), offered(mk("drop", "vadd"), Tx.MR))
        assertEquals(listOf("→ Scope (null) [staff] > scope", "← Vendor added (client changes it) [client] > vadd", "→ Dropped (null) [client] > drop"), offered(mk("cok", "vadd")))
        assertEquals(listOf("→ स्कोप (null) [staff] > scope", "← व्हेंडरने जोडले (ग्राहकाला बदल हवा) [client] > vadd", "→ वगळले (null) [client] > drop"), offered(mk("cok", "vadd"), Tx.MR))
        // born anything else, Client approved goes back to Client wish
        assertEquals(listOf("→ Scope (null) [staff] > scope", "← Client wish (client changes it) [client] > wish", "→ Dropped (null) [client] > drop"), offered(mk("cok")))
        assertEquals(offered(mk("cok")), offered(mk("cok", "wish")))
        // the table itself is never changed by a resolve
        assertEquals("wish", MarkLife.MOVES.getValue("drop").single().to); assertNull(MarkLife.MOVES.getValue("drop").single().hint)
        assertEquals(MarkLife.Label.BACK_WISH, MarkLife.MOVES.getValue("cok")[1].label)
        assertTrue(MarkLife.MOVE_TABLE.none { it.amber })
    }

    // ------------------------------------------------------------------ a move

    @Test fun `Existing to Client wish and back is the mark that left -- the same keys, in the same order, with the same values`() {
        val e = mark(linkedMapOf("locked" to false, "future" to mapOf("deep" to listOf(1, null))))
        val before = LinkedHashMap(e.extra)
        val p = SurveyPrep.fresh(3319.0, 2678.0, 2677.0); p.surface("front").marks.add(e)
        val file = SurveyPrepFile.encode(p, 3319.0, 2678.0, 2677.0).toString()

        val there = MarkLife.move(e, "wish", PhaseTools.WISH_LIST, at, act = "remove")
        assertEquals(MarkLife.Moved(null, "exist", "wish", false), there); assertTrue(there.ok)
        // the prototype's own order: what was there, then life, lifeAt, lifeAs, act
        assertEquals(listOf("locked", "future", "life", "lifeAt", "lifeAs", "act"), keys(e))
        assertEquals(listOf<Any?>(false, mapOf("deep" to listOf(1, null)), "wish", stamp, "client", "remove"), e.extra.values.toList())
        assertTrue(file != SurveyPrepFile.encode(p, 3319.0, 2678.0, 2677.0).toString())

        val back = MarkLife.move(e, "exist", PhaseTools.WISH_LIST, at)
        assertEquals(MarkLife.Moved(null, "wish", "exist", false), back)
        assertEquals(before, e.extra); assertEquals(before.keys.toList(), keys(e))
        assertEquals(file, SurveyPrepFile.encode(p, 3319.0, 2678.0, 2677.0).toString(), "the prep file is byte for byte the one before")
        assertEquals("exist", MarkLife.lifeOf(e)); assertNull(MarkLife.actOf(e))
    }

    @Test fun `a move replaces the mark's extra map and never changes it in place -- an undo copy keeps the mark as it was`() {
        val e = mark(mapOf("locked" to false)); val undo = e.copy(); val old = e.extra
        assertTrue(MarkLife.move(e, "wish", 1, at).ok)
        assertNotSame(old, e.extra); assertSame(old, undo.extra)
        assertEquals(mapOf("locked" to false), undo.extra); assertEquals("exist", MarkLife.lifeOf(undo)); assertEquals("wish", MarkLife.lifeOf(e))
        // and a refused move writes nothing at all: the same map, untouched
        val kept = e.extra
        assertFalse(MarkLife.move(e, "scope", 1, at).ok); assertSame(kept, e.extra)
    }

    @Test fun `the act is written only on Existing to Client wish -- the one given, else Replace -- and stays with the mark`() {
        for ((given, want) in listOf("replace" to "replace", "shift" to "shift", "repair" to "repair", "remove" to "remove", null to "replace", "nonsense" to "replace", "" to "replace")) {
            val e = mark(); MarkLife.move(e, "wish", 1, at, act = given)
            assertEquals(want, e.extra["act"], "$given")
        }
        // it rides every later move, and a later move's act is not taken
        val e = mark(); MarkLife.move(e, "wish", 1, at, act = "shift")
        assertTrue(MarkLife.move(e, "vrev", 2, at, act = "remove").ok); assertEquals("shift", MarkLife.actOf(e))
        assertTrue(MarkLife.move(e, "vok", 2, at).ok); assertTrue(MarkLife.move(e, "cok", 2, at).ok)
        assertTrue(MarkLife.move(e, "wish", 2, at, act = "repair").ok); assertEquals("shift", MarkLife.actOf(e), "← Client wish is not Existing → Client wish")
        // a mark born a wish or Vendor added has nothing to replace: no act, at placement or after
        val w = mark(); MarkLife.placeAs(w, "wish", 1, at); assertFalse("act" in w.extra)
        MarkLife.move(w, "drop", 1, at, why = "changed her mind", act = "remove"); MarkLife.move(w, "wish", 1, at, act = "remove")
        assertFalse("act" in w.extra)
        // back at Existing the act goes with the life
        val b = mark(); MarkLife.move(b, "wish", 1, at, act = "repair"); MarkLife.move(b, "exist", 1, at); assertEquals(emptyMap(), b.extra)
    }

    @Test fun `a why of 3 to 140 characters -- needed by a move that asks one, trimmed, cut, and gone with the next move that has none`() {
        val e = mk("wish")
        for (short in listOf("", "  ", "ab", " ab ", "\n a\t")) {
            val kept = e.extra
            assertEquals(MarkLife.Moved(Refusal.WHY, "wish", "drop"), MarkLife.move(e, "drop", 1, at, why = short), "'$short'")
            assertSame(kept, e.extra)
        }
        assertEquals("Why? — at least 3 characters", MarkLife.refusalText(Tx.EN, e, MarkLife.move(e, "drop", 1, at)))
        assertEquals("का? — किमान 3 अक्षरे", MarkLife.refusalText(Tx.MR, e, MarkLife.move(e, "drop", 1, at)))
        assertEquals("Why? — at least 3 characters", MarkLife.whyNeeded(Tx.EN))
        // three characters once trimmed is enough, and it is stored trimmed
        assertTrue(MarkLife.move(e, "drop", 1, at, why = "  not needed  ").ok)
        assertEquals("not needed", MarkLife.whyOf(e)); assertEquals(listOf("life", "lifeAt", "lifeAs", "why"), keys(e))
        // Restore asks no why: the old one is deleted
        assertTrue(MarkLife.move(e, "wish", 1, at).ok); assertFalse("why" in e.extra); assertEquals("staff", MarkLife.lifeAs(e))
        assertTrue(MarkLife.move(e, "drop", 1, at, why = "abc").ok); assertEquals("abc", MarkLife.whyOf(e))
        // cut to 140
        val long = mk("wish"); assertTrue(MarkLife.move(long, "drop", 1, at, why = "x".repeat(200)).ok); assertEquals(140, MarkLife.whyOf(long).length)
        val exact = mk("wish"); assertTrue(MarkLife.move(exact, "drop", 1, at, why = "y".repeat(140)).ok); assertEquals("y".repeat(140), MarkLife.whyOf(exact))
        // a why handed to a move that asks none is not stored
        val n = mark(); assertTrue(MarkLife.move(n, "wish", 1, at, why = "because").ok); assertFalse("why" in n.extra)
        // every move that needs a why is refused without one, and only those
        for (m in MarkLife.MOVE_TABLE) {
            val x = mk(m.from); val r = MarkLife.move(x, if (m.restore) "wish" else m.to, null, at)
            assertEquals(if (m.why) Refusal.WHY else null, r.refusal, "${m.from} > ${m.to}")
        }
    }

    @Test fun `the whole journey of a surveyed window - each move writes the state, the stamp and who decided`() {
        val e = mark()
        fun step(to: String, stage: Int, why: String = "") = MarkLife.move(e, to, stage, at, why = why).let { "${it.from}>${it.to}${if (it.amber) "!" else ""} ${e.extra}" }
        assertEquals("exist>wish {life=wish, lifeAt=$stamp, lifeAs=client, act=replace}", step("wish", 1))
        assertEquals("wish>drop {life=drop, lifeAt=$stamp, lifeAs=client, act=replace, why=not needed}", step("drop", 1, "not needed"))
        assertEquals("drop>wish {life=wish, lifeAt=$stamp, lifeAs=staff, act=replace}", step("wish", 1))
        assertEquals("wish>vrev {life=vrev, lifeAt=$stamp, lifeAs=staff, act=replace}", step("vrev", 2))
        assertEquals("vrev>vok {life=vok, lifeAt=$stamp, lifeAs=vendor, act=replace}", step("vok", 2))
        assertEquals("vok>cok {life=cok, lifeAt=$stamp, lifeAs=client, act=replace}", step("cok", 2))
        // after the estimate (Revisions), into and out of Scope is told to the office
        assertEquals("cok>scope! {life=scope, lifeAt=$stamp, lifeAs=staff, act=replace, locked=true}", step("scope", 4))
        assertEquals("scope>cok! {life=cok, lifeAt=$stamp, lifeAs=staff, act=replace, locked=false}", step("cok", 4))
        assertEquals("cok>wish {life=wish, lifeAt=$stamp, lifeAs=client, act=replace, locked=false}", step("wish", 2))
        assertEquals("After the estimate — tell the office", MarkLife.afterEstimate(Tx.EN)); assertEquals("अंदाजपत्रकानंतर — ऑफिसला कळवा", MarkLife.afterEstimate(Tx.MR))
        // the stamp is the time of each move
        val later = Instant.parse("2026-12-31T18:45:07.900Z")
        assertTrue(MarkLife.move(e, "vrev", 2, later).ok); assertEquals("2027-01-01T00:15:07+05:30", MarkLife.lifeAt(e))
    }

    @Test fun `Scope locks the mark and only leaving Scope unlocks it -- no other move touches the lock`() {
        val e = mk("cok")
        assertTrue(MarkLife.move(e, "scope", 2, at).ok); assertTrue(SurveyPrep.isLocked(e)); assertEquals(true, e.extra["locked"])
        assertFalse(MarkLife.move(e, "scope", 2, at).ok, "Scope offers only Out of scope")
        assertEquals(listOf("cok"), MarkLife.allowedMoves(e, 2).map { it.to })
        assertTrue(MarkLife.move(e, "cok", 2, at).ok); assertFalse(SurveyPrep.isLocked(e)); assertEquals(false, e.extra["locked"], "unlocked, the key kept")
        // before the estimate (Vendor quoted) neither is told to the office; unread, neither
        assertFalse(MarkLife.move(mk("cok"), "scope", 2, at).amber); assertFalse(MarkLife.move(mk("cok"), "scope", null, at).amber)
        assertTrue(MarkLife.move(mk("cok"), "scope", 4, at).amber); assertTrue(MarkLife.move(mk("scope"), "cok", 4, at).amber)
        assertFalse(MarkLife.move(mk("cok"), "drop", 4, at, why = "too dear").amber)
        // a lock a person set stays through every other move, and a move never sets one
        val locked = mark(mapOf("locked" to true))
        assertTrue(MarkLife.move(locked, "wish", 1, at).ok); assertTrue(SurveyPrep.isLocked(locked))
        assertTrue(MarkLife.move(locked, "exist", 1, at).ok); assertEquals(mapOf("locked" to true), locked.extra)
        val free = mark(); MarkLife.move(free, "wish", 1, at); assertFalse("locked" in free.extra)
    }

    @Test fun `Survey refuses every move, with its words -- and on the phone so does Client approved, with the approval lock's`() {
        for (life in MarkLife.IDS) for (m in MarkLife.MOVES.getValue(life)) {
            val e = mk(life); val kept = e.extra
            assertEquals(MarkLife.Moved(Refusal.SURVEY, life, m.to), MarkLife.move(e, m.to, PhaseTools.SURVEY, at, why = "because"), "$life > ${m.to}")
            assertEquals(MarkLife.Moved(Refusal.APPROVED, life, m.to), MarkLife.move(e, m.to, PhaseTools.APPROVED, at, why = "because"), "$life > ${m.to}")
            assertSame(kept, e.extra)
        }
        val e = mk("wish")
        assertEquals("Finish the survey first — the stage moves to Client wish list on S5", MarkLife.refusalText(Tx.EN, e, MarkLife.move(e, "vrev", 0, at)))
        assertEquals("आधी सर्व्हे पूर्ण करा — S5 वर टप्पा \"ग्राहकाची इच्छा-यादी\" वर जातो", MarkLife.refusalText(Tx.MR, e, MarkLife.move(e, "vrev", 0, at)))
        assertEquals("Approved — changes go through change requests", MarkLife.refusalText(Tx.EN, e, MarkLife.move(e, "vrev", 5, at)))
        assertEquals("मंजूर झाले — पुढचे बदल \"बदल विनंती\" मधूनच होतात", MarkLife.refusalText(Tx.MR, e, MarkLife.move(e, "vrev", 5, at)))
        assertEquals("Quote stage not read yet — S5 decides when online", MarkLife.unreadFoot(Tx.EN))
        assertEquals("कोटेशनचा टप्पा अजून वाचलेला नाही — ऑनलाइन आल्यावर S5 ठरवते", MarkLife.unreadFoot(Tx.MR))
    }

    @Test fun `a move the mark is not offered is refused, naming where it is and where it cannot go -- and a Note has no state`() {
        val e = mk("wish")
        // the Wish list phase does not send a wish to Vendor review; Estimation moves nothing
        val r = MarkLife.move(e, "vrev", 1, at)
        assertEquals(MarkLife.Moved(Refusal.NOT_OFFERED, "wish", "vrev"), r); assertFalse(r.ok)
        assertEquals("FW1: Client wish cannot move to Vendor review", MarkLife.refusalText(Tx.EN, e, r))
        assertEquals("FW1: ग्राहकाची इच्छा वरून व्हेंडर तपासणी वर जाता येत नाही", MarkLife.refusalText(Tx.MR, e, r))
        assertEquals("FW1: Client wish cannot move to Vendor review", MarkLife.refusalText(Tx.EN, e, MarkLife.move(e, "vrev", 3, at)))
        // a state the table has no row to, a state that is none, the state it is in
        for (to in listOf("scope", "cok", "vadd", "zzz", "wish")) assertEquals(Refusal.NOT_OFFERED, MarkLife.move(e, to, null, at).refusal, to)
        assertEquals("FW1: Client wish cannot move to Existing", MarkLife.refusalText(Tx.EN, e, MarkLife.move(e, "zzz", null, at)), "a state that is none is named as Existing, as the prototype names it")
        // born a wish, it cannot go back to Existing; a move is found by its RESOLVED target
        assertEquals(Refusal.NOT_OFFERED, MarkLife.move(mk("wish", "wish"), "exist", 1, at).refusal)
        assertEquals(Refusal.NOT_OFFERED, MarkLife.move(mk("drop", "vadd"), "wish", 2, at).refusal); assertTrue(MarkLife.move(mk("drop", "vadd"), "vadd", 2, at).ok)
        assertEquals(Refusal.NOT_OFFERED, MarkLife.move(mk("cok", "vadd"), "wish", 2, at).refusal); assertTrue(MarkLife.move(mk("cok", "vadd"), "vadd", 2, at).ok)
        assertNull(MarkLife.refusalText(Tx.EN, e, MarkLife.Moved(null, "wish", "drop")))
        // a Note
        val n = mark(t = "NOTE", code = "FNOTE1")
        for (st in STAGES) assertEquals(MarkLife.Moved(Refusal.NOTE, "exist", "wish"), MarkLife.move(n, "wish", st, at), "stage $st")
        assertEquals("A Note has no state", MarkLife.refusalText(Tx.EN, n, MarkLife.move(n, "wish", 1, at)))
        assertEquals("नोंदीला स्थिती नसते", MarkLife.refusalText(Tx.MR, n, MarkLife.move(n, "wish", 1, at)))
        assertEquals(emptyMap(), n.extra)
    }

    @Test fun `an unknown life is kept until a move writes a known one over it, where it was -- and unknown keys stay whole`() {
        val e = mark(linkedMapOf("life" to "zzz", "future" to listOf(mapOf("a" to 1)), "lifeAs" to "robot"))
        assertEquals("exist", MarkLife.lifeOf(e)); assertEquals(listOf("wish"), MarkLife.allowedMoves(e, 1).map { it.to })
        assertEquals(Refusal.NOT_OFFERED, MarkLife.move(e, "vrev", 2, at).refusal); assertEquals("zzz", e.extra["life"], "a refused move leaves it")
        assertTrue(MarkLife.move(e, "wish", 1, at).ok)
        assertEquals(listOf("life", "future", "lifeAs", "lifeAt", "act"), keys(e)); assertEquals("wish", e.extra["life"]); assertEquals("client", e.extra["lifeAs"])
        assertEquals(listOf(mapOf("a" to 1)), e.extra["future"])
    }

    // ------------------------------------------------------------------ placing

    @Test fun `placing as a Client wish or as Vendor added sets born and life once, with the stamp and who decided`() {
        val w = mark(mapOf("locked" to false))
        assertEquals("wish", MarkLife.placeAs(w, "wish", PhaseTools.WISH_LIST, at))
        assertEquals(listOf("locked", "born", "life", "lifeAt", "lifeAs"), keys(w))
        assertEquals(listOf<Any?>(false, "wish", "wish", stamp, "client"), w.extra.values.toList())
        val v = mark(); val undo = v.copy()
        assertEquals("vadd", MarkLife.placeAs(v, "vadd", PhaseTools.VENDOR_REVIEW, at))
        assertEquals("{born=vadd, life=vadd, lifeAt=$stamp, lifeAs=vendor}", v.extra.toString()); assertEquals(emptyMap(), undo.extra)
        assertEquals("wish", MarkLife.bornOf(w)); assertEquals("wish", MarkLife.lifeOf(w)); assertEquals("vadd", MarkLife.bornOf(v))
    }

    @Test fun `nothing is written for a mark placed as Existing, at Survey, or for a Note -- it is a v52 mark`() {
        for (asId in listOf(null, "exist", "", "cok", "scope", "drop", "Wish")) {
            val e = mark(); val kept = e.extra
            assertNull(MarkLife.placeAs(e, asId, 1, at), "$asId"); assertSame(kept, e.extra)
        }
        for (asId in listOf("wish", "vadd")) {
            val e = mark(); val kept = e.extra
            assertNull(MarkLife.placeAs(e, asId, PhaseTools.SURVEY, at), "at Survey every mark is Existing"); assertSame(kept, e.extra)
            val n = mark(t = "NOTE"); assertNull(MarkLife.placeAs(n, asId, 1, at), "a Note has no state"); assertEquals(emptyMap(), n.extra)
        }
        // unread, what the cycling button picked is placed
        val u = mark(); assertEquals("wish", MarkLife.placeAs(u, PhaseTools.asFor(null, "wish"), null, at)); assertEquals("wish", MarkLife.lifeOf(u))
        val x = mark(); assertNull(MarkLife.placeAs(x, PhaseTools.asFor(null, null), null, at)); assertEquals(emptyMap(), x.extra)
    }

    @Test fun `a Vendor added mark's journey - approved, back to Vendor added, dropped, restored to Vendor added`() {
        val v = mark(); MarkLife.placeAs(v, "vadd", 2, at)
        fun step(to: String, why: String = "") = MarkLife.move(v, to, 2, at, why = why).let { "${it.from}>${it.to} ${v.extra}" }
        assertEquals("vadd>cok {born=vadd, life=cok, lifeAt=$stamp, lifeAs=client}", step("cok"))
        assertEquals(listOf("scope", "vadd", "drop"), MarkLife.allowedMoves(v, 2).map { it.to })
        assertEquals("cok>vadd {born=vadd, life=vadd, lifeAt=$stamp, lifeAs=client}", step("vadd"))
        assertEquals("vadd>drop {born=vadd, life=drop, lifeAt=$stamp, lifeAs=client, why=no budget}", step("drop", "no budget"))
        assertEquals("drop>vadd {born=vadd, life=vadd, lifeAt=$stamp, lifeAs=staff}", step("vadd"))
        assertEquals("vadd", MarkLife.bornOf(v))
    }

    @Test fun `another act on a mark born Existing that has left Existing -- and on no other mark`() {
        val e = mark(); MarkLife.move(e, "wish", 1, at); val undo = e.copy()
        assertTrue(MarkLife.setAct(e, "shift")); assertEquals("shift", MarkLife.actOf(e)); assertEquals("replace", MarkLife.actOf(undo))
        assertEquals(listOf("life", "lifeAt", "lifeAs", "act"), keys(e), "the key keeps its place")
        val kept = e.extra
        assertFalse(MarkLife.setAct(e, "shift"), "the act it has"); assertFalse(MarkLife.setAct(e, "paint"), "not one of the four"); assertSame(kept, e.extra)
        assertFalse(MarkLife.setAct(mark(), "remove"), "still Existing: the act goes with → Client wish")
        assertFalse(MarkLife.setAct(mk("wish", "wish"), "remove"), "born a wish: nothing to replace")
        assertFalse(MarkLife.setAct(mk("vadd", "vadd"), "remove")); assertFalse(MarkLife.setAct(mk("wish", t = "NOTE"), "remove"))
        // a wish read from a file with no act takes one
        val bare = mk("vrev"); assertTrue(MarkLife.setAct(bare, "replace")); assertEquals("replace", MarkLife.actOf(bare))
    }

    // ------------------------------------------------------------------ the note

    @Test fun `a note is kept as typed, Office only is kept only with a note, and a blank note deletes both keys`() {
        val before: Map<String, Any?> = linkedMapOf("locked" to false, "life" to "wish")
        val typed = "  install UPVC window \n"
        val n = MarkLife.withNote(before, typed, office = false)
        assertEquals(listOf("locked", "life", "note"), n.keys.toList()); assertEquals(typed, n["note"], "as typed, not trimmed")
        assertEquals(listOf("locked", "life"), before.keys.toList(), "a new map: the old one is not changed"); assertNotSame(before, n)
        val o = MarkLife.withNote(n, "close with brickwork", office = true)
        assertEquals(listOf("locked", "life", "note", "vis"), o.keys.toList()); assertEquals("office", o["vis"]); assertTrue(MarkLife.isOffice(o))
        // back to Client & vendors: the default is never stored
        assertEquals(listOf("locked", "life", "note"), MarkLife.withNote(o, "close with brickwork", office = false).keys.toList())
        // a blank note takes Office only with it
        for (blank in listOf("", "   ", "\n\t ")) assertEquals(before, MarkLife.withNote(o, blank, office = true), "'$blank'")
        assertEquals(before.keys.toList(), MarkLife.withNote(o, "", office = true).keys.toList())
        assertEquals(emptyMap(), MarkLife.withNote(emptyMap(), "", office = true))
        // on a mark and on a Line, through the same rule
        val e = mark(before); val undo = e.copy(); MarkLife.setNote(e, "UPVC", true)
        assertEquals("UPVC", MarkLife.noteOf(e.extra)); assertEquals("", MarkLife.noteOf(undo.extra))
        val line = SurveyPrep.Meas("m", "line", "F-M1", mutableListOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(900.0, 0.0)), mapOf("locked" to true))
        MarkLife.setNote(line, "skirting here", false); assertEquals(linkedMapOf<String, Any?>("locked" to true, "note" to "skirting here"), line.extra)
        MarkLife.setNote(line, " ", false); assertEquals(mapOf("locked" to true), line.extra)
    }

    @Test fun `a note is cut at 280 characters, counted as the prototype counts them -- a letter, an emoji and a Marathi sign one each`() {
        assertEquals(280, (MarkLife.withNote(emptyMap(), "x".repeat(281), false)["note"] as String).length)
        val exact = "y".repeat(280); assertSame(exact, MarkLife.withNote(emptyMap(), exact, false)["note"])
        // an emoji is two UTF-16 units and ONE character: 281 of them are cut to 280, never to 140 and never through the middle of one
        val cut = MarkLife.withNote(emptyMap(), "😀".repeat(281), false)["note"] as String
        assertEquals(280, cut.codePointCount(0, cut.length)); assertEquals(560, cut.length); assertEquals("😀".repeat(280), cut)
        val mixed = "खिडकी " + "😀".repeat(300)
        val m = MarkLife.withNote(emptyMap(), mixed, false)["note"] as String
        assertEquals(280, m.codePointCount(0, m.length)); assertTrue(m.startsWith("खिडकी 😀")); assertTrue(mixed.startsWith(m))
    }

    // ------------------------------------------------------------------ delete, unlock, clear

    @Test fun `the delete control reads Delete, Drop or Restore -- and a mark past Existing is never simply deleted`() {
        val rows = listOf(
            Triple("an Existing mark", mark(), "DELETE PLAIN Delete हटवा"),
            Triple("a Note", mark(t = "NOTE"), "DELETE PLAIN Delete हटवा"),
            Triple("a Note carrying a life", mk("wish", t = "NOTE"), "DELETE PLAIN Delete हटवा"),
            Triple("a wish on a surveyed mark", mk("wish"), "DROP DROP_SHEET Drop ड्रॉप"),
            Triple("a mark born a wish", mk("wish", "wish"), "DROP DROP_SHEET Drop ड्रॉप"),
            Triple("a Vendor added mark", mk("vadd", "vadd"), "DROP DROP_SHEET Drop ड्रॉप"),
            Triple("in Vendor review", mk("vrev"), "DROP DROP_SHEET Drop ड्रॉप"),
            Triple("Client approved", mk("cok"), "DROP DROP_SHEET Drop ड्रॉप"),
            Triple("a Dropped mark", mk("drop"), "RESTORE RESTORE Restore परत आणा"),
            // locked: v52's own delete answers, and refuses with "locked — unlock first" (a Scope mark is locked)
            Triple("a locked wish", mark(mapOf("locked" to true, "life" to "wish")), "DROP PLAIN Drop ड्रॉप"),
            Triple("in Scope", mark(mapOf("life" to "scope", "locked" to true)), "DROP PLAIN Drop ड्रॉप"),
            Triple("a locked Dropped mark", mark(mapOf("locked" to true, "life" to "drop")), "RESTORE PLAIN Restore परत आणा"),
            Triple("an unknown life", mark(mapOf("life" to "zzz")), "DELETE PLAIN Delete हटवा"),
        )
        for ((what, e, want) in rows)
            assertEquals(want, "${MarkLife.deleteWord(e)} ${MarkLife.deleteAct(e)} ${MarkLife.deleteLabel(Tx.EN, e)} ${MarkLife.deleteLabel(Tx.MR, e)}", what)
        assertEquals(listOf(DeleteWord.DELETE, DeleteWord.DROP, DeleteWord.RESTORE), DeleteWord.values().toList())
        assertEquals(listOf(DeleteAct.PLAIN, DeleteAct.RESTORE, DeleteAct.DROP_SHEET), DeleteAct.values().toList())
    }

    @Test fun `Restore moves a Dropped mark back at once to what it was born, and says so -- or says why it cannot`() {
        val e = mk("drop"); e.extra = e.extra + ("why" to "no budget")
        assertEquals(MarkLife.Moved(null, "drop", "wish"), MarkLife.restore(e, PhaseTools.WISH_LIST, at))
        assertEquals("FW1 restored to Client wish", MarkLife.restoredText(Tx.EN, e)); assertEquals("FW1 पुन्हा \"ग्राहकाची इच्छा\" मध्ये आणले", MarkLife.restoredText(Tx.MR, e))
        assertFalse("why" in e.extra)
        val v = mk("drop", "vadd")
        assertEquals(MarkLife.Moved(null, "drop", "vadd"), MarkLife.restore(v, PhaseTools.VENDOR_REVIEW, at)); assertEquals("FW1 restored to Vendor added", MarkLife.restoredText(Tx.EN, v))
        // at Survey, Estimated and Client approved nothing moves
        assertEquals(Refusal.SURVEY, MarkLife.restore(mk("drop"), 0, at).refusal)
        val est = mk("drop"); val r = MarkLife.restore(est, 3, at)
        assertEquals(Refusal.NOT_OFFERED, r.refusal); assertEquals("FW1: Dropped cannot move to Client wish", MarkLife.refusalText(Tx.EN, est, r))
        assertEquals(Refusal.APPROVED, MarkLife.restore(mk("drop"), 5, at).refusal)
    }

    @Test fun `a Scope mark is not unlocked by its padlock or by Unlock all -- it is taken Out of scope in its sheet`() {
        val s = mark(mapOf("life" to "scope", "locked" to true))
        assertEquals("FW1 is in Scope — take it Out of scope in its sheet", MarkLife.unlockRefusal(Tx.EN, s))
        assertEquals("FW1 स्कोपमध्ये आहे — त्याच्या तपशीलात \"स्कोपबाहेर\" करा", MarkLife.unlockRefusal(Tx.MR, s))
        assertFalse(MarkLife.unlockable(s))
        for (life in MarkLife.IDS - "scope") { assertNull(MarkLife.unlockRefusal(Tx.EN, mk(life)), life); assertTrue(MarkLife.unlockable(mk(life)), life) }
        assertTrue(MarkLife.unlockable(mk("scope", t = "NOTE")), "a Note has no state")
        assertEquals("1 Scope mark stays locked", MarkLife.scopeKeptText(Tx.EN, 1)); assertEquals("3 Scope marks stay locked", MarkLife.scopeKeptText(Tx.EN, 3))
        assertEquals("स्कोपमधील 3 मार्क लॉक राहतात", MarkLife.scopeKeptText(Tx.MR, 3))
    }

    @Test fun `a clear keeps every locked item and every mark past Existing, and says what it kept`() {
        // spared: what Clear this surface, Clear whole room and Start over keep
        assertFalse(MarkLife.spared(mark())); assertTrue(MarkLife.spared(mark(mapOf("locked" to true))))
        for (life in MarkLife.STORED) assertTrue(MarkLife.spared(mk(life)), life)
        assertTrue(MarkLife.spared(mk("wish", "wish"))); assertTrue(MarkLife.spared(mk("drop")), "a Dropped mark is kept too: it is restored or left, never swept")
        assertFalse(MarkLife.spared(mk("wish", t = "NOTE")), "a Note is deleted like an Existing mark"); assertFalse(MarkLife.spared(mark(mapOf("life" to "zzz"))))
        fun line(locked: Boolean) = SurveyPrep.Meas("m", "line", "F-M1", mutableListOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(9.0, 0.0)), mapOf("locked" to locked, "note" to "x"))
        fun chain(locked: Boolean) = SurveyPrep.Chain("k", "FK1", "x", mutableListOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(9.0, 0.0)), extra = mapOf("locked" to locked))
        assertEquals(listOf(true, false, true, false), listOf(MarkLife.spared(line(true)), MarkLife.spared(line(false)), MarkLife.spared(chain(true)), MarkLife.spared(chain(false))))
        // clearText(n, lockedKept, pastKept)
        assertEquals("3 items cleared", MarkLife.clearText(Tx.EN, 3, 0, 0)); assertEquals("1 item cleared", MarkLife.clearText(Tx.EN, 1, 0, 0))
        assertEquals("Cleared 1 item · 2 locked kept", MarkLife.clearText(Tx.EN, 1, 2, 0))
        assertEquals("Cleared 3 items · 1 locked kept · 2 marks past Existing kept — drop them one by one", MarkLife.clearText(Tx.EN, 3, 1, 2))
        assertEquals("Cleared 0 items · 1 mark past Existing kept — drop them one by one", MarkLife.clearText(Tx.EN, 0, 0, 1))
        assertEquals("3 आयटम काढले", MarkLife.clearText(Tx.MR, 3, 0, 0))
        assertEquals("3 आयटम काढले · 1 लॉक केलेले ठेवले · \"सध्याचे\" च्या पुढे गेलेले 2 मार्क ठेवले — ते एकेक करून वगळा", MarkLife.clearText(Tx.MR, 3, 1, 2))
        assertEquals("1 mark past Existing kept — drop them one by one", MarkLife.pastKeptText(Tx.EN, 1))
        assertEquals("2 marks past Existing kept — drop them one by one", MarkLife.pastKeptText(Tx.EN, 2))
        assertEquals("\"सध्याचे\" च्या पुढे गेलेले 2 मार्क ठेवले — ते एकेक करून वगळा", MarkLife.pastKeptText(Tx.MR, 2))
    }

    // ------------------------------------------------------------------ the counts, the stamp

    @Test fun `the roll-up counts a prep's marks by state -- every state present, Notes left out -- and adds rooms up`() {
        val p = SurveyPrep.fresh(3319.0, 2678.0, 2677.0)
        p.surface("front").marks.addAll(listOf(mk("exist"), mk("wish"), mk("wish", "wish"), mark(t = "NOTE"), mk("drop"), mark(mapOf("life" to "zzz"))))
        p.surface("floor").marks.add(mk("scope"))
        val c = MarkLife.countLife(p)
        assertEquals("{exist=2, wish=2, vrev=0, vok=0, vadd=0, cok=0, scope=1, drop=1}", c.toString())
        assertEquals(MarkLife.IDS, c.keys.toList())
        val q = SurveyPrep.fresh(3319.0, 2678.0, 2677.0); q.surface("ceil").marks.addAll(listOf(mk("vadd", "vadd"), mk("wish")))
        assertEquals("{exist=2, wish=3, vrev=0, vok=0, vadd=1, cok=0, scope=1, drop=1}", MarkLife.rollUp(listOf(p, q)).toString())
        assertEquals("{exist=0, wish=0, vrev=0, vok=0, vadd=0, cok=0, scope=0, drop=0}", MarkLife.rollUp(emptyList()).toString())
        assertEquals("{exist=0, wish=0, vrev=0, vok=0, vadd=0, cok=0, scope=0, drop=0}", MarkLife.countLife(SurveyPrep.fresh(3319.0, 2678.0, 2677.0)).toString())
        assertEquals("{exist=1, wish=5, vrev=0, vok=0, vadd=0, cok=0, scope=0, drop=0}", MarkLife.addCounts(mapOf("wish" to 2, "exist" to 1), mapOf("wish" to 3, "junk" to 9)).toString())
    }

    @Test fun `the stamp is IST to the second, with its offset`() {
        assertEquals("2026-10-16T06:37:09+05:30", MarkLife.istIso(Instant.parse("2026-10-16T01:07:09.500Z")))
        assertEquals("2027-01-01T00:15:00+05:30", MarkLife.istIso(Instant.parse("2026-12-31T18:45:00Z")))
        assertEquals("2026-03-05T09:04:03+05:30", MarkLife.istIso(Instant.parse("2026-03-05T03:34:03Z")))
        // QuoteStage reads it back as the instant it names, to the second
        assertEquals(Instant.parse("2026-10-16T01:07:09Z"), QuoteStage.instantOf(MarkLife.istIso(Instant.parse("2026-10-16T01:07:09.500Z"))))
    }
}
