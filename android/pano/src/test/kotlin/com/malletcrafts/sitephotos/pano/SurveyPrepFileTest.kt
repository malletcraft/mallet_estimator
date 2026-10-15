package com.malletcrafts.sitephotos.pano

import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The prep file read tolerantly and written back whole (v52 slice 2): a key
 * this build does not read survives a save, one bad value costs that value
 * and not the room, and a file that is no prep is never mistaken for an
 * empty one.
 */
class SurveyPrepFileTest {

    private fun surface(extra: String = "", els: String = "[]") =
        """{"bottom":[[192,640],[768,640]],"top":[[192,160],[768,160]],"left":[[192,600],[192,200]],"right":[[768,600],[768,200]],""" +
            """"set":{"bottom":true,"top":true,"left":true,"right":false},"els":$els$extra}"""

    /** A prep as this build has always written it. */
    private val v1 = """{"v":1,"L":3319,"W":2678,"H":2677,"w":{""" +
        listOf("front", "right", "back", "left", "floor", "ceil").joinToString(",") { s ->
            "\"$s\":" + surface(els = if (s == "front") """[{"id":"a1","t":"D","code":"FD1","x0":300,"x1":1200,"y0":0,"y1":2100.5}]""" else "[]")
        } + """},"actual":{"FD1.w":905,"FD1.h":2050.5}}"""

    /** Numbers compared as numbers, whatever type the parser gave them. */
    private fun norm(v: Any?): Any? = when (v) {
        is Number -> v.toDouble()
        is Map<*, *> -> v.entries.associate { (k, x) -> k to norm(x) }
        is List<*> -> v.map { norm(it) }
        else -> v
    }

    @Suppress("UNCHECKED_CAST")
    private fun parse(t: String) = MiniJson.parse(t) as Map<String, Any?>

    private fun roundTrip(t: String): Pair<Map<String, Any?>, Map<String, Any?>> {
        val o = parse(t); val d = assertNotNull(SurveyPrepFile.decode(o)); val z = assertNotNull(d.size)
        return o to SurveyPrepFile.encode(d.prep, z[0], z[1], z[2])
    }

    @Test fun `a v1 prep reads whole and re-encodes to the same content, in the same key order`() {
        val o = parse(v1); val d = assertNotNull(SurveyPrepFile.decode(o))
        assertTrue(d.problems.isEmpty(), d.problems.toString())
        assertEquals(listOf(3319.0, 2678.0, 2677.0), d.size!!.toList())
        assertEquals(1, d.prep.surface("front").marks.size)
        assertFalse(d.prep.surface("front").set["right"]!!)
        val back = SurveyPrepFile.encode(d.prep, 3319.0, 2678.0, 2677.0)
        assertEquals(norm(o), norm(back))
        assertEquals(listOf("v", "L", "W", "H", "w", "actual"), back.keys.toList())
        @Suppress("UNCHECKED_CAST") val f = (back["w"] as Map<String, Map<String, Any?>>).getValue("front")
        assertEquals(listOf("bottom", "top", "left", "right", "set", "els"), f.keys.toList())
    }

    /** As the prototype (v52) stores a room: locks, tapes, Lines, chains and a reference. */
    private val proto = """{"v":1,"L":5458.5,"W":3310,"H":2705,"note":{"by":"v52"},"w":{""" +
        listOf("front", "right", "back", "left", "floor", "ceil").joinToString(",") { s ->
            "\"$s\":" + if (s != "front") surface() else surface(
                extra = ""","lock":true,"meas":[{"id":"m1","code":"FT1","kind":"tape","locked":false,"pts":[[100,200],[900,800]]}],""" +
                    """"chains":[{"id":"k1","code":"FK1","axis":"x","locked":true,"nodes":[[0,900],[600,900]],"snaps":[null,"l"]}],""" +
                    """"ref":{"on":true,"q":[[1,2],[3,4],[5,6],[7,8]],"w":1000,"h":800,"locked":false,"shown":false}""",
                els = """[{"id":"a1","t":"D","code":"FD1","x0":300,"x1":1200,"y0":0,"y1":2100,"locked":true}]""")
        } + ""","wall360":{"anything":[1,2]}},"actual":{"FD1.w":905}}"""

    @Test fun `a prototype prep with meas, chains, ref, shown and locks round-trips every field, each at its own level`() {
        val (o, back) = roundTrip(proto)
        assertEquals(norm(o), norm(back))
        @Suppress("UNCHECKED_CAST") val w = back["w"] as Map<String, Map<String, Any?>>
        assertEquals(listOf("bottom", "top", "left", "right", "set", "els", "lock", "meas", "chains", "ref"), w.getValue("front").keys.toList())
        assertTrue("wall360" in w.keys)
        assertEquals("v52", (back["note"] as Map<*, *>)["by"])
        // a decoded prep knows nothing of them, and keeps them anyway
        val d = SurveyPrepFile.decode(parse(proto))!!
        assertEquals(setOf("lock", "meas", "chains", "ref"), d.prep.surface("front").extra.keys)
        assertEquals(mapOf("locked" to true), d.prep.surface("front").marks[0].extra)
    }

    @Test fun `copies keep what the file carried -- an undo or a Save never drops it`() {
        val p = SurveyPrepFile.decode(parse(proto))!!.prep.copy().copy()
        assertEquals(setOf("lock", "meas", "chains", "ref"), p.surface("front").extra.keys)
        assertEquals(true, p.surface("front").marks[0].copy().extra["locked"])
        assertEquals(setOf("note"), p.extra.keys)
        assertEquals(setOf("wall360"), p.extraSurfaces.keys)
        // and a mark added afterwards writes no keys of its own beyond the seven
        p.addMark("front", "W", 3310.0, 2705.0, "n1")
        @Suppress("UNCHECKED_CAST") val els = ((SurveyPrepFile.encode(p, 5458.5, 3310.0, 2705.0)["w"] as Map<String, Map<String, Any?>>)
            .getValue("front")["els"] as List<Map<String, Any?>>)
        assertEquals(SurveyPrepFile.MARK_KEYS, els.last().keys)
    }

    @Test fun `a prep missing one surface keeps the other five, starts that one again, and says so`() {
        val o = parse(v1).toMutableMap()
        o["w"] = (o["w"] as Map<*, *>).filterKeys { it != "back" }
        val d = assertNotNull(SurveyPrepFile.decode(o))
        assertEquals(1, d.problems.size); assertTrue(d.problems[0].startsWith("Back wall: missing"), d.problems[0])
        assertEquals(SurveyPrep.SURFACES, d.prep.surfaces.keys.toList())
        assertFalse(d.prep.surface("back").done())
        assertEquals(1, d.prep.surface("front").marks.size)
        assertEquals(2, d.prep.actual.size)
        val fresh = SurveyPrep.fresh(3319.0, 2678.0, 2677.0).surface("back").lines.getValue("top")
        assertEquals(fresh[0].toList(), d.prep.surface("back").lines.getValue("top")[0].toList())
    }

    @Test fun `one malformed line, mark or reading costs only itself`() {
        val bad = v1.replace(""""left":[[192,600],[192,200]],"right":[[768,600],[768,200]],"set":{"bottom":true,"top":true,"left":true,"right":false},"els":[{"id":"a1"""",
            """"left":[[192,"x"],[192,200]],"right":[[768,600],[768,200]],"set":{"bottom":true,"top":true,"left":true,"right":false},"els":[{"id":"z","t":"W","code":"FW1","x0":"oops","x1":1,"y0":1,"y1":2},{"id":"a1"""")
            .replace(""""FD1.h":2050.5""", """"FD1.h":{"v":2050}""")
        assertTrue(bad != v1)
        val d = assertNotNull(SurveyPrepFile.decode(parse(bad)))
        assertEquals(3, d.problems.size, d.problems.toString())
        val f = d.prep.surface("front")
        assertEquals(listOf("FD1"), f.marks.map { it.code })
        assertFalse(f.set["left"]!!); assertTrue(f.set["bottom"]!!)
        assertEquals(setOf("FD1.w"), d.prep.actual.keys)
        assertTrue(d.problems.any { it.contains("left corner") })
        assertTrue(d.problems.any { it.contains("FW1") })
    }

    @Test fun `what is not a prep is null, never an empty prep`() {
        assertNull(SurveyPrepFile.decode(parse("""{"v":1}""")))
        assertNull(SurveyPrepFile.decode(parse("""{"w":[1,2]}""")))
        assertNull(SurveyPrepFile.sizeOf(parse("""{"L":200,"W":2000,"H":2000}""")))
    }

    @Test fun `numbers written as strings, as org json would read them, are taken`() {
        val d = assertNotNull(SurveyPrepFile.decode(parse(v1.replace("\"x0\":300", "\"x0\":\"300\""))))
        assertTrue(d.problems.isEmpty()); assertEquals(300.0, d.prep.surface("front").marks[0].x0)
    }

    @Test fun `a file set aside is named for its capture and the time, and the screen says what happened`() {
        val ist = ZoneId.of("Asia/Kolkata")
        // 15 Oct 2026 17:15:00 UTC = 22:45:00 IST
        val t = java.time.ZonedDateTime.of(2026, 10, 15, 17, 15, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()
        assertEquals("MCAP-1.bad-20261015-224500.json", SurveyPrepFile.asideName("MCAP-1", t, ist))
        assertEquals("MCAP-1.bad-20261015-224500-2.json", SurveyPrepFile.asideName("MCAP-1", t, ist, 2))
        assertTrue(SurveyPrepFile.isAside("MCAP-1", "MCAP-1.bad-20261015-224500.json"))
        assertFalse(SurveyPrepFile.isAside("MCAP-1", "MCAP-1.sync.json"))
        assertFalse(SurveyPrepFile.isAside("MCAP-1", "MCAP-12.bad-20261015-224500.json"))

        assertNull(SurveyPrepFile.openedNote(SurveyPrepFile.Opened.Missing))
        assertEquals("Nothing saved yet", SurveyPrepFile.savedText(SurveyPrepFile.Opened.Missing))
        val d = SurveyPrepFile.decode(parse(v1))!!
        assertNull(SurveyPrepFile.openedNote(SurveyPrepFile.Opened.Restored(d)))
        assertEquals("Restored ✓", SurveyPrepFile.savedText(SurveyPrepFile.Opened.Restored(d)))
        assertEquals("The saved prep could not be read. It is kept as X.json, and this room starts fresh.",
            SurveyPrepFile.openedNote(SurveyPrepFile.Opened.Unreadable("X.json")))
        assertTrue(SurveyPrepFile.openedNote(SurveyPrepFile.Opened.Unreadable(null))!!.contains("not overwritten"))
        val o = parse(v1).toMutableMap(); o["w"] = (o["w"] as Map<*, *>).filterKeys { it != "ceil" }
        val part = SurveyPrepFile.Opened.Salvaged(SurveyPrepFile.decode(o)!!, "Y.json")
        assertEquals("Part of the saved prep could not be read (1 thing: Ceiling: missing, started again). The file as it was is kept as Y.json.",
            SurveyPrepFile.openedNote(part))
    }
}
