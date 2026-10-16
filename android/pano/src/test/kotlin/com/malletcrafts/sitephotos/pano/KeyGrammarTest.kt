package com.malletcrafts.sitephotos.pano

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * EVERY READING KEY THIS APP CAN SEND, S5 ACCEPTS (v52 slice 4). S5 QUOTE
 * refuses the WHOLE readings PATCH for a room on one key its grammar does not
 * take, so a phone sending a key it refuses would stop every reading of that
 * room from syncing. The pattern is copied VERBATIM from S5's
 * functions/_lib/asis.js (KEY_RE, live on stage since 5acac1d); change it here
 * only when S5's changes.
 */
class KeyGrammarTest {

    private val keyRe = Regex("""^[A-Za-z][A-Za-z0-9-]{0,23}\.([A-Za-z]{1,8}|[0-9]{1,3})$""")

    private val room = 3319.0 to 2678.0
    private val h = 2677.0

    @Test fun `every code prefix with every measure key the checklists can ask for`() {
        val keys = mutableSetOf<String>()
        for (s in SurveyPrep.SURFACES) {
            val ab = SurveyPrep.size(s, room.first, room.second, h)
            for (t in SurveyPrep.types(s)) {
                // a mark in each corner and in the middle, so every nearer-side key comes out
                val w = if (SurveyPrep.isPoint(t.letter)) SurveyPrep.SPOT else 300.0
                for ((x, y) in listOf(10.0 to 10.0, ab[0] - w - 10 to ab[1] - w - 10, ab[0] / 2 to ab[1] / 2, 10.0 to ab[1] - w - 10, ab[0] - w - 10 to 10.0)) {
                    for (n in listOf(1, 9, 999)) {
                        val code = SurveyPrep.CODE.getValue(s) + t.letter + n
                        val e = SurveyPrep.Mark("x", t.letter, code, x, x + w, y, y + w)
                        for (m in SurveyPrep.measures0(s, e, ab[0], ab[1])) keys.add(code + "." + m.key)
                    }
                }
            }
        }
        val bad = keys.filterNot { keyRe.matches(it) }
        assertTrue(bad.isEmpty(), "S5 would refuse: $bad")
        // and every centre key appears, so the grammar was really tried on them
        for (k in listOf("cl", "cr", "cs", "ct", "cf", "ck")) assertTrue(keys.any { it.endsWith(".$k") }, k)
        for (k in listOf("w", "h", "l", "r", "s", "t", "f", "k", "p", "d")) assertTrue(keys.any { it.endsWith(".$k") }, k)
    }

    @Test fun `what normPoints writes is accepted too`() {
        val p = SurveyPrep.fresh(3319.0, 2678.0, 2677.0)
        p.surface("front").marks.add(SurveyPrep.Mark("a", "E", "FE12", 300.0, 450.0, 300.0, 450.0))
        p.surface("ceil").marks.add(SurveyPrep.Mark("b", "F", "CF3", 1000.0, 1150.0, 2000.0, 2150.0))
        p.actual.putAll(mapOf("FE12.r" to 1.0, "FE12.t" to 2.0, "CF3.f" to 3.0, "CF3.l" to 4.0))
        val out = SurveyPrep.normPoints(p).prep.actual.keys
        assertTrue(out.isNotEmpty() && out.all { keyRe.matches(it) }, out.toString())
    }

    @Test fun `a Line's len and an area's per, on every surface, up to the highest code, are accepted (v52 slice 9)`() {
        val p = SurveyPrep.fresh(3319.0, 2678.0, 2677.0)
        val keys = mutableListOf<String>()
        for (s in SurveyPrep.SURFACES) {
            for (i in 0 until 3) {
                keys += p.keysOf(p.addMeas(s, SurveyMeas.LINE, listOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(500.0, 0.0)), "l$s$i"))
                keys += p.keysOf(p.addMeas(s, SurveyMeas.AREA, listOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(500.0, 0.0), doubleArrayOf(500.0, 500.0)), "a$s$i"))
                // a tape has no key at all: nothing of it is ever sent as a reading
                assertTrue(p.keysOf(p.addMeas(s, SurveyMeas.TAPE, listOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(500.0, 0.0)), "t$s$i")).isEmpty())
            }
        }
        assertTrue(keys.any { it.endsWith(".len") } && keys.any { it.endsWith(".per") })
        for (code in listOf("F-M1", "G-M999", "C-M12345", "L-M99999999999999999999")) for (k in listOf("len", "per")) keys += "$code.$k"
        val bad = keys.filterNot { keyRe.matches(it) }
        assertTrue(bad.isEmpty(), "S5 would refuse: $bad")
    }

    @Test fun `a chain's segments and total, FK1_1 and FK1_t, on every surface, up to the highest code, are accepted (v52 slice 11)`() {
        val p = SurveyPrep.fresh(3319.0, 2678.0, 2677.0)
        val keys = mutableListOf<String>()
        for (s in SurveyPrep.SURFACES) for (i in 0 until 3) {
            val nodes = (0..(4 + i)).map { doubleArrayOf(it * 400.0, 900.0) }
            keys += p.keysOf(p.addChain(s, "x", nodes, nodes.map { null }, "k$s$i"))
        }
        assertTrue("FK1.1" in keys && "FK1.t" in keys && "GK3.6" in keys && "CK2.t" in keys, keys.toString())
        // a chain of 999 segments, and the longest code the grammar takes
        for (code in listOf("FK1", "RK12", "BK999", "LK99999999999999999999")) for (k in listOf("1", "9", "999", "t")) keys += "$code.$k"
        val bad = keys.filterNot { keyRe.matches(it) }
        assertTrue(bad.isEmpty(), "S5 would refuse: $bad")
    }

    @Test fun `the copied pattern still refuses what S5 refuses`() {
        for (k in listOf("FE1", "1FE.l", "FE1.", ".l", "FE1.cl.x", "FE1.toolongforakey", "F E1.l", "FE1.1234")) assertFalse(keyRe.matches(k), k)
        for (k in listOf("FE1.cl", "F-M1.len", "F-M2.per", "G-M3.len", "C-M1.per", "FK1.1", "FK1.t", "GP9.ck")) assertTrue(keyRe.matches(k), k)
        // a chain of a thousand segments is refused: the phone's segment keys stop at three digits
        assertFalse(keyRe.matches("FK1.1000"))
    }
}
