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
        assertTrue(SurveyPrepFile.openedNote(SurveyPrepFile.Opened.Salvaged(part.decoded, null))!!.endsWith(
            "could not be copied aside: nothing here is saved, so it is not overwritten."))
    }

    // ------------------------------------------------------------ opening (the store's decision)

    /** The disk, faked: what was copied or moved aside, and whether either may succeed. */
    private class Disk(val copyOk: Boolean = true, val moveOk: Boolean = true, val throws: Boolean = false) {
        val copied = mutableListOf<String>(); val moved = mutableListOf<String>()
        fun copy(): String? { if (throws) error("disk full"); return if (copyOk) "C.json".also { copied.add(it) } else null }
        fun move(): String? { if (throws) error("disk full"); return if (moveOk) "M.json".also { moved.add(it) } else null }
    }

    private fun open(text: String?, disk: Disk, exists: Boolean = text != null) =
        SurveyPrepFile.open(text, exists, { t -> runCatching { parse(t) }.getOrNull() }, disk::copy, disk::move)

    /** Plain maps back to JSON text, enough for what encode writes. */
    private fun write(v: Any?): String = when (v) {
        null -> "null"
        is String -> "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        is Map<*, *> -> v.entries.joinToString(",", "{", "}") { (k, x) -> write(k.toString()) + ":" + write(x) }
        is List<*> -> v.joinToString(",", "[", "]") { write(it) }
        else -> v.toString()
    }

    private val salvageable: String = v1.replace(""""ceil":""", """"ceilx":""")

    @Test fun `no file is Missing -- nothing set aside, nothing held, and it is saved on open`() {
        val d = Disk(); val r = open(null, d)
        assertTrue(r.opened is SurveyPrepFile.Opened.Missing); assertFalse(r.hold)
        assertTrue(d.copied.isEmpty() && d.moved.isEmpty())
        assertTrue(SurveyPrepFile.saveOnOpen(r.opened))
    }

    @Test fun `a whole prep is Restored and nothing is copied`() {
        val d = Disk(); val r = open(v1, d)
        assertTrue(r.opened is SurveyPrepFile.Opened.Restored); assertFalse(r.hold)
        assertTrue(d.copied.isEmpty() && d.moved.isEmpty())
        assertFalse(SurveyPrepFile.saveOnOpen(r.opened))
    }

    @Test fun `a truncated file is MOVED aside, never treated as Missing and saved over`() {
        val d = Disk(); val r = open(v1.take(v1.length / 2), d)
        val o = r.opened as SurveyPrepFile.Opened.Unreadable
        assertEquals("M.json", o.aside); assertFalse(r.hold)
        assertEquals(listOf("M.json"), d.moved); assertTrue(d.copied.isEmpty())
        assertFalse(SurveyPrepFile.saveOnOpen(o))
        // and a JSON object that is no prep, and a file that exists but cannot be read, go the same way
        assertTrue(open("""{"v":1}""", Disk()).opened is SurveyPrepFile.Opened.Unreadable)
        assertTrue(open(null, Disk(), exists = true).opened is SurveyPrepFile.Opened.Unreadable)
    }

    @Test fun `an unreadable file that cannot be moved aside is HELD -- every save refused`() {
        for (d in listOf(Disk(moveOk = false), Disk(throws = true))) {
            val r = open("{not json", d)
            assertNull((r.opened as SurveyPrepFile.Opened.Unreadable).aside)
            assertTrue(r.hold)
            assertFalse(SurveyPrepFile.saveOnOpen(r.opened))
        }
    }

    @Test fun `a prep read in part is COPIED aside, not held, and saved on open so the live file heals`() {
        val d = Disk(); val r = open(salvageable, d)
        val o = r.opened as SurveyPrepFile.Opened.Salvaged
        assertEquals("C.json", o.aside); assertFalse(r.hold)
        assertEquals(listOf("C.json"), d.copied); assertTrue(d.moved.isEmpty())
        assertTrue(o.decoded.problems.isNotEmpty())
        assertTrue(SurveyPrepFile.saveOnOpen(o))
        // healed: what the screen then writes opens Restored, so no second copy is made
        val z = o.decoded.size!!
        val text = write(SurveyPrepFile.encode(o.decoded.prep, z[0], z[1], z[2]))
        val d2 = Disk(); assertTrue(open(text, d2).opened is SurveyPrepFile.Opened.Restored)
        assertTrue(d2.copied.isEmpty())
    }

    @Test fun `a prep read in part whose copy fails is HELD and never saved over -- the only copy of what was lost`() {
        for (d in listOf(Disk(copyOk = false), Disk(throws = true))) {
            val r = open(salvageable, d)
            val o = r.opened as SurveyPrepFile.Opened.Salvaged
            assertNull(o.aside); assertTrue(r.hold)
            assertFalse(SurveyPrepFile.saveOnOpen(o))
        }
    }

    // ------------------------------------------------------------ v52 slice 4: older points made spots on opening

    /** APK 0.3.212's prep: a wall Electric point 150 x 150, measured to its edge (l, s) with its w and h. */
    private val v1Point = v1.replaceFirst("""[{"id":"a1","t":"D"""",
        """[{"id":"e9","t":"E","code":"FE9","x0":300,"x1":450,"y0":300,"y1":450,"locked":true},{"id":"a1","t":"D"""")
        .replace(""""actual":{""", """"actual":{"FE9.l":300,"FE9.s":300,"FE9.w":150,"FE9.h":150,""")

    @Test fun `a v1 prep with a 150 mm wall point opens as a 50 mm spot with cl and cs, saved once, then left alone`() {
        val r = open(v1Point, Disk())
        val o = r.opened as SurveyPrepFile.Opened.Restored
        assertEquals(1, o.decoded.points); assertEquals(1, SurveyPrepFile.pointsOf(o))
        assertEquals("1 point re-centred", SurveyPrepFile.pointsNote(o))
        assertTrue(o.decoded.pointLog.single().startsWith("front FE9: 150 x 150 re-centred"), o.decoded.pointLog.toString())
        val e = o.decoded.prep.surface("front").marks.first { it.code == "FE9" }
        assertEquals(listOf(350.0, 400.0, 350.0, 400.0), listOf(e.x0, e.x1, e.y0, e.y1))
        assertEquals(true, e.extra["locked"], "what the file carried for the mark is kept")
        assertEquals(listOf(150.0, 150.0), e.extra[SurveyPrep.WAS], "and the size its edge readings converted with, for a later one")
        assertEquals(mapOf("cl" to 375.0, "cs" to 375.0), e.extra[SurveyPrep.CONV])
        assertEquals(mapOf("FE9.cl" to 375.0, "FE9.cs" to 375.0, "FD1.w" to 905.0, "FD1.h" to 2050.5), o.decoded.prep.actual)
        assertTrue(SurveyPrepFile.saveOnOpen(o), "the conversion is written at once")
        // what the screen writes then opens Restored with nothing to do: once only
        val z = o.decoded.size!!
        val again = open(write(SurveyPrepFile.encode(o.decoded.prep, z[0], z[1], z[2])), Disk()).opened as SurveyPrepFile.Opened.Restored
        assertEquals(0, again.decoded.points); assertNull(SurveyPrepFile.pointsNote(again)); assertFalse(SurveyPrepFile.convertedOf(again))
        val e2 = again.decoded.prep.surface("front").marks.first { it.code == "FE9" }
        assertEquals(listOf(150.0, 150.0), (e2.extra[SurveyPrep.WAS] as List<*>).map { (it as Number).toDouble() }, "the size survives a save")
        assertFalse(SurveyPrepFile.saveOnOpen(again))
        assertEquals(o.decoded.prep.actual, again.decoded.prep.actual)
    }

    @Test fun `the readings sync then sends the old key as a delete and the new one as a set, each on its own base`() {
        val o = open(v1Point, Disk()).opened as SurveyPrepFile.Opened.Restored
        // what the phone last saw on the web: the edge readings, before the update
        val seen = mapOf("FE9.l" to Readings.Seen(300.0, "t1"), "FE9.s" to Readings.Seen(300.0, "t2"),
            "FE9.w" to Readings.Seen(150.0, "t3"), "FE9.h" to Readings.Seen(150.0, "t4"), "FD1.w" to Readings.Seen(905.0, "t5"))
        val push = Readings.push(o.decoded.prep.actual, seen)
        assertEquals(mapOf("FD1.h" to 2050.5, "FE9.cl" to 375.0, "FE9.cs" to 375.0, "FE9.h" to null, "FE9.l" to null,
            "FE9.s" to null, "FE9.w" to null), push.set)
        assertEquals("t1", push.base["FE9.l"]); assertNull(push.base["FE9.cl"])
        // the web still holding the edge readings does not bring them back while the deletes are unsent
        val remote = mapOf("FE9.l" to Readings.Remote(300.0, "phone", "t1"), "FE9.s" to Readings.Remote(300.0, "phone", "t2"),
            "FE9.w" to Readings.Remote(150.0, "phone", "t3"), "FE9.h" to Readings.Remote(150.0, "phone", "t4"),
            "FD1.w" to Readings.Remote(905.0, "phone", "t5"))
        val m = Readings.pull(o.decoded.prep.actual, seen, remote)
        assertTrue(m.taken.isEmpty(), m.taken.toString()); assertNull(m.actual["FE9.l"]); assertNull(m.actual["FE9.w"])
        assertEquals(375.0, m.actual["FE9.cl"])
    }

    // ------------------------------------------------------------ the JSON library, faked

    /** A JSON library of its own: an object, an array and a null that are not plain maps, lists or null. */
    private data class FObj(val m: List<Pair<String, Any>>)
    private data class FArr(val l: List<Any>)
    private object FNull

    private val fake = SurveyPrepFile.JsonShape(
        isNull = { it == null || it === FNull },
        entries = { (it as? FObj)?.m },
        items = { (it as? FArr)?.l },
        newObject = { FObj(it) },
        newArray = { FArr(it) },
        nul = FNull,
    )

    @Test fun `the library tree converts to plain maps and back -- JSON null stays null in lists, nested unknown objects stay whole`() {
        val tree = FObj(listOf(
            "v" to 1,
            "chains" to FArr(listOf(FObj(listOf("id" to "k1", "snaps" to FArr(listOf(FNull, "l")))))),
            "note" to FObj(listOf("deep" to FObj(listOf("b" to FArr(listOf(1, 2.5, true)), "z" to FNull)))),
            "gone" to FNull,
        ))
        val p = assertNotNull(SurveyPrepFile.plainObject(tree, fake))
        assertEquals(listOf("v", "chains", "note", "gone"), p.keys.toList())
        assertTrue(p.containsKey("gone")); assertNull(p["gone"])
        assertEquals(listOf(null, "l"), ((p["chains"] as List<*>)[0] as Map<*, *>)["snaps"])
        assertEquals(mapOf("deep" to mapOf("b" to listOf(1, 2.5, true), "z" to null)), p["note"])
        // and back: null is the library's null, never the text "null"
        assertEquals(tree, SurveyPrepFile.unplain(p, fake))
        assertNull(SurveyPrepFile.plainObject(FArr(listOf(1)), fake))
    }

    @Test fun `a prototype prep through the library and back keeps every field, nulls included`() {
        val tree = SurveyPrepFile.unplain(parse(proto), fake)
        val o = assertNotNull(SurveyPrepFile.plainObject(tree, fake))
        val d = assertNotNull(SurveyPrepFile.decode(o)); val z = d.size!!
        val back = SurveyPrepFile.plainObject(SurveyPrepFile.unplain(SurveyPrepFile.encode(d.prep, z[0], z[1], z[2]), fake), fake)
        assertEquals(norm(parse(proto)), norm(back))
        @Suppress("UNCHECKED_CAST") val front = ((back!!["w"] as Map<String, Map<String, Any?>>).getValue("front"))
        assertEquals(listOf(null, "l"), ((front["chains"] as List<*>)[0] as Map<*, *>)["snaps"])
    }

    @Test fun `a lock set here (a mark's and the lines') and the code counter survive a save and an open (v52 slice 8)`() {
        val p = SurveyPrepFile.decode(parse(v1))!!.prep
        p.addMark("front", "W", 2678.0, 2677.0, "w1")                     // FW1: the counter records it
        assertEquals(true, SurveyPrepEdit.setLocked(p, "a1", true))
        assertTrue(SurveyPrepEdit.setLinesLocked(p, "right", true))
        assertTrue(p.deleteMark("w1"))
        val o = SurveyPrepFile.encode(p, 3319.0, 2678.0, 2677.0)
        @Suppress("UNCHECKED_CAST") val w = o["w"] as Map<String, Map<String, Any?>>
        // where the prototype keeps them: the mark's "locked", the surface's "lock"
        assertEquals(true, (w.getValue("front")["els"] as List<Map<String, Any?>>)[0]["locked"])
        assertEquals(true, w.getValue("right")["lock"])
        // through text, as the store writes it, and back
        val back = SurveyPrepFile.decode(parse(write(o)))!!
        assertTrue(back.problems.isEmpty(), back.problems.toString())
        assertTrue(SurveyPrep.isLocked(back.prep.markById("a1")!!.second))
        assertTrue(SurveyPrep.linesLocked(back.prep.surface("right")))
        assertFalse(SurveyPrep.linesLocked(back.prep.surface("front")))
        assertEquals("FW2", back.prep.nextCode("front", "W"), "FW1 was deleted, and is not handed out again after an open")
        // unlocked, it is written as the prototype writes it: false, not dropped
        SurveyPrepEdit.setLocked(back.prep, "a1", false)
        @Suppress("UNCHECKED_CAST") val els = ((SurveyPrepFile.encode(back.prep, 3319.0, 2678.0, 2677.0)["w"] as Map<String, Map<String, Any?>>)
            .getValue("front")["els"] as List<Map<String, Any?>>)
        assertEquals(false, els[0]["locked"])
    }
}
