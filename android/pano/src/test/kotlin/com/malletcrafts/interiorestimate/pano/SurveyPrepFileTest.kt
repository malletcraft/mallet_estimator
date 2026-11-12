package com.malletcrafts.interiorestimate.pano

import java.time.ZoneId
import com.malletcrafts.interiorestimate.pano.i18n.Tx
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
        // meas is read since slice 9 and chains since slice 11, each written straight after els; the rest stay where they were
        assertEquals(listOf("bottom", "top", "left", "right", "set", "els", "meas", "chains", "lock", "ref"), w.getValue("front").keys.toList())
        assertTrue("wall360" in w.keys)
        assertEquals("v52", (back["note"] as Map<*, *>)["by"])
        // a decoded prep knows nothing of the others, and keeps them anyway
        val d = SurveyPrepFile.decode(parse(proto))!!
        assertEquals(setOf("lock", "ref"), d.prep.surface("front").extra.keys)
        // the chain is read as one (v52 slice 11): its axis, points, snaps -- a null kept a null -- and its lock
        val k = d.prep.surface("front").chains.single()
        assertEquals("FK1", k.code); assertEquals("x", k.axis); assertEquals(listOf(null, "l"), k.snaps)
        assertTrue(SurveyPrep.isLocked(k)); assertEquals(listOf(listOf(0.0, 900.0), listOf(600.0, 900.0)), k.nodes.map { it.toList() })
        @Suppress("UNCHECKED_CAST") val c0 = (w.getValue("front")["chains"] as List<Map<String, Any?>>)[0]
        assertEquals(listOf("id", "code", "axis", "locked", "nodes", "snaps"), c0.keys.toList())
        assertEquals(mapOf("locked" to true), d.prep.surface("front").marks[0].extra)
        // the tape is read as one (v52 slice 9), its locked flag kept with it in the prototype's own key order
        val t = d.prep.surface("front").meas.single()
        assertEquals("tape", t.kind); assertEquals("FT1", t.code); assertEquals(mapOf("locked" to false), t.extra)
        @Suppress("UNCHECKED_CAST") val m0 = (w.getValue("front")["meas"] as List<Map<String, Any?>>)[0]
        assertEquals(listOf("id", "code", "kind", "locked", "pts"), m0.keys.toList())
    }

    @Test fun `copies keep what the file carried -- an undo or a Save never drops it`() {
        val p = SurveyPrepFile.decode(parse(proto))!!.prep.copy().copy()
        assertEquals(setOf("lock", "ref"), p.surface("front").extra.keys)
        assertEquals(1, p.surface("front").meas.size)
        assertEquals(1, p.surface("front").chains.size)
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
        assertEquals(1, d.problems.size); assertTrue(d.problems[0](Tx.EN).startsWith("Back wall: missing"), d.problems[0](Tx.EN))
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
        assertEquals(3, d.problems.size, d.problems.map { it(Tx.EN) }.toString())
        val f = d.prep.surface("front")
        assertEquals(listOf("FD1"), f.marks.map { it.code })
        assertFalse(f.set["left"]!!); assertTrue(f.set["bottom"]!!)
        assertEquals(setOf("FD1.w"), d.prep.actual.keys)
        assertTrue(d.problems.any { it(Tx.EN).contains("left corner") })
        assertTrue(d.problems.any { it(Tx.EN).contains("FW1") })
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
        assertEquals("Nothing saved yet", SurveyPrepFile.savedText(SurveyPrepFile.Opened.Missing)(Tx.EN))
        val d = SurveyPrepFile.decode(parse(v1))!!
        assertNull(SurveyPrepFile.openedNote(SurveyPrepFile.Opened.Restored(d)))
        assertEquals("Restored ✓", SurveyPrepFile.savedText(SurveyPrepFile.Opened.Restored(d))(Tx.EN))
        assertEquals("The saved prep could not be read. It is kept as X.json, and this room starts fresh.",
            SurveyPrepFile.openedNote(SurveyPrepFile.Opened.Unreadable("X.json"))!!(Tx.EN))
        assertTrue(SurveyPrepFile.openedNote(SurveyPrepFile.Opened.Unreadable(null))!!(Tx.EN).contains("not overwritten"))
        val o = parse(v1).toMutableMap(); o["w"] = (o["w"] as Map<*, *>).filterKeys { it != "ceil" }
        val part = SurveyPrepFile.Opened.Salvaged(SurveyPrepFile.decode(o)!!, "Y.json")
        assertEquals("Part of the saved prep could not be read (1 thing: Ceiling: missing, started again). The file as it was is kept as Y.json.",
            SurveyPrepFile.openedNote(part)!!(Tx.EN))
        assertTrue(SurveyPrepFile.openedNote(SurveyPrepFile.Opened.Salvaged(part.decoded, null))!!(Tx.EN).endsWith(
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
        assertEquals("1 point re-centred", SurveyPrepFile.pointsNote(o)!!(Tx.EN))
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

    // ------------------------------------------------------------------ port 62 slice L1: a mark's life keys

    /**
     * As the prototype (v53) stores a room once wishes are marked on it: a surveyed window the client wants replaced, a
     * door she dropped, a new wardrobe born a wish, a vendor's TV unit, a Note for the office, an Other with its trade,
     * a point in Scope -- each with only the keys that are not default, in the order the prototype wrote them -- a
     * state a newer build added, a Line with a trade and a note, and three of the ten new types.
     */
    private val v53 = """{"v":1,"L":5458.5,"W":3310,"H":2705,"w":{""" +
        listOf("front", "right", "back", "left", "floor", "ceil").joinToString(",") { s ->
            "\"$s\":" + when (s) {
                "front" -> surface(
                    extra = ""","meas":[{"id":"m1","code":"F-M1","kind":"line","locked":false,"cat":"painting","note":"skirting here","vis":"office","pts":[[100,200],[900,200]]}]""",
                    els = "[" + listOf(
                        """{"id":"a1","t":"W","code":"FW1","x0":1000,"x1":2200,"y0":900,"y1":2100,"locked":false,"life":"wish","lifeAt":"2026-11-11T10:30:00+05:30","lifeAs":"client","act":"replace","note":"install UPVC window"}""",
                        """{"id":"a2","t":"D","code":"FD1","x0":300,"x1":1200,"y0":0,"y1":2100,"life":"drop","lifeAt":"2026-11-11T10:31:00+05:30","lifeAs":"client","act":"remove","why":"she changed her mind"}""",
                        """{"id":"a3","t":"CRP","code":"FCRP1","x0":150,"x1":1350,"y0":0,"y1":2100,"born":"wish","life":"wish","lifeAt":"2026-11-11T10:32:00+05:30","lifeAs":"client"}""",
                        """{"id":"a4","t":"CRP","code":"FCRP2","x0":1500,"x1":3300,"y0":400,"y1":850,"sub":"tv","born":"vadd","life":"cok","lifeAt":"2026-11-12T09:00:00+05:30","lifeAs":"client"}""",
                        """{"id":"a5","t":"NOTE","code":"FNOTE1","x0":1630,"x1":1680,"y0":1327,"y1":1377,"cat":"carpentry","note":"keep the old shelf","vis":"office"}""",
                        """{"id":"a6","t":"X","code":"FX1","x0":100,"x1":700,"y0":100,"y1":700,"cat":"plumbing","life":"zzz","later":{"by":"v70","n":[1,null]}}""",
                        """{"id":"a7","t":"E","code":"FE1","x0":325,"x1":375,"y0":325,"y1":375,"life":"scope","lifeAt":"2026-11-13T18:05:09+05:30","lifeAs":"staff","act":"shift","locked":true}""",
                    ).joinToString(",") + "]")
                "ceil" -> surface(els = """[{"id":"c1","t":"CDP","code":"CCDP1","x0":300,"x1":3010,"y0":300,"y1":5158.5,"sub":"full","born":"wish","life":"wish","lifeAt":"2026-11-11T10:40:00+05:30","lifeAs":"client"}]""")
                else -> surface()
            }
        } + """},"actual":{"FW1.w":1210}}"""

    @Suppress("UNCHECKED_CAST")
    private fun elsOf(tree: Map<String, Any?>, s: String) = ((tree["w"] as Map<String, Map<String, Any?>>).getValue(s)["els"] as List<Map<String, Any?>>)

    @Test fun `a v53 prep -- wishes, acts, whys, notes, trades, sub-kinds, new types -- round-trips every key, on its own mark, in its own order`() {
        val (o, back) = roundTrip(v53)
        assertEquals(norm(o), norm(back))
        for (s in listOf("front", "ceil")) assertEquals(elsOf(o, s).map { it.keys.toList() }, elsOf(back, s).map { it.keys.toList() }, "$s: every mark's keys, in the order they came")
        // each of the ten keys is where the prototype put it: after the seven, in the order it wrote them
        assertEquals(listOf("id", "t", "code", "x0", "x1", "y0", "y1", "locked", "life", "lifeAt", "lifeAs", "act", "note"), elsOf(back, "front")[0].keys.toList())
        assertEquals(listOf("id", "t", "code", "x0", "x1", "y0", "y1", "sub", "born", "life", "lifeAt", "lifeAs"), elsOf(back, "front")[3].keys.toList())
        @Suppress("UNCHECKED_CAST") val m0 = ((back["w"] as Map<String, Map<String, Any?>>).getValue("front")["meas"] as List<Map<String, Any?>>)[0]
        assertEquals(listOf("id", "code", "kind", "locked", "cat", "note", "vis", "pts"), m0.keys.toList())

        // and the build reads them: the state, what it was born, the act, the why, the note and its audience, the trade
        val d = SurveyPrepFile.decode(parse(v53))!!
        assertTrue(d.problems.isEmpty(), d.problems.map { it(Tx.EN) }.toString())
        val f = d.prep.surface("front").marks.associateBy { it.id }
        fun row(id: String) = f.getValue(id).let { e ->
            listOf(MarkLife.lifeOf(e), MarkLife.bornOf(e), MarkLife.actOf(e), MarkLife.whyOf(e), MarkLife.noteOf(e.extra), MarkLife.isOffice(e.extra), Trades.of("front", e)).joinToString(" | ")
        }
        assertEquals("wish | exist | replace |  | install UPVC window | false | doors-windows", row("a1"))
        assertEquals("drop | exist | remove | she changed her mind |  | false | doors-windows", row("a2"))
        assertEquals("wish | wish | null |  |  | false | carpentry", row("a3"))
        assertEquals("cok | vadd | null |  |  | false | carpentry", row("a4"))
        assertEquals("exist | exist | null |  | keep the old shelf | true | carpentry", row("a5"))
        assertEquals("exist | exist | null |  |  | false | plumbing", row("a6"), "a state this build does not know reads Existing")
        assertEquals("scope | exist | shift |  |  | false | electrical", row("a7"))
        assertEquals("zzz", f.getValue("a6").extra["life"], "... and is kept as it came")
        assertEquals(norm(mapOf("by" to "v70", "n" to listOf(1, null))), norm(f.getValue("a6").extra["later"]), "an unknown key beside the life keys stays whole")
        assertEquals("tv", Trades.subOf(f.getValue("a4").extra)); assertNull(Trades.subOf(f.getValue("a3").extra), "the first sub-kind is never stored")
        assertTrue(SurveyPrep.isLocked(f.getValue("a7"))); assertEquals("2026-11-13T18:05:09+05:30", MarkLife.lifeAt(f.getValue("a7"))); assertEquals("staff", MarkLife.lifeAs(f.getValue("a7")))
        val c1 = d.prep.surface("ceil").marks.single()
        assertEquals("false-ceiling", Trades.of("ceil", c1)); assertEquals("wish", MarkLife.bornOf(c1)); assertEquals("full", Trades.subOf(c1.extra))
        val line = d.prep.surface("front").meas.single()
        assertEquals("painting", Trades.of("front", line)); assertEquals("skirting here", MarkLife.noteOf(line.extra)); assertTrue(MarkLife.isOffice(line.extra))
        assertEquals("{exist=1, wish=3, vrev=0, vok=0, vadd=0, cok=1, scope=1, drop=1}", MarkLife.countLife(d.prep).toString(), "the Note is not counted; the unknown state counts as Existing")

        // a copy for undo, a move and a save keep every key of every OTHER mark, and the moved mark's unknown ones
        val p = d.prep.copy()
        assertTrue(MarkLife.move(p.markById("a1")!!.second, "drop", PhaseTools.WISH_LIST, java.time.Instant.parse("2026-11-14T05:00:00Z"), why = "too dear").ok)
        val saved = SurveyPrepFile.encode(p, 5458.5, 3310.0, 2705.0)
        assertEquals(elsOf(back, "front").drop(1).map { norm(it) }, elsOf(saved, "front").drop(1).map { norm(it) })
        assertEquals(listOf("id", "t", "code", "x0", "x1", "y0", "y1", "locked", "life", "lifeAt", "lifeAs", "act", "note", "why"), elsOf(saved, "front")[0].keys.toList())
        assertEquals("drop", elsOf(saved, "front")[0]["life"]); assertEquals("install UPVC window", elsOf(saved, "front")[0]["note"])
        assertEquals("wish", MarkLife.lifeOf(d.prep.markById("a1")!!.second), "the prep the copy was taken from is untouched")
    }

    @Test fun `a v52 prep saves unchanged -- every mark reads Existing, and nothing writes a key whose value is its default`() {
        for (text in listOf(v1, proto)) {
            val o = parse(text); val d = SurveyPrepFile.decode(o)!!; val z = d.size!!
            val before = write(SurveyPrepFile.encode(d.prep, z[0], z[1], z[2]))
            assertEquals(norm(o), norm(parse(before)), "the file as it was")
            val at = java.time.Instant.parse("2026-11-11T05:00:00Z")
            for (s in SurveyPrep.SURFACES) {
                for (e in d.prep.surface(s).marks) {
                    // read: a v52 mark is Existing, born Existing, with no act, no why, no note; its trade is its type's
                    assertEquals(listOf("exist", "exist", null, "", "", false), listOf(MarkLife.lifeOf(e), MarkLife.bornOf(e), MarkLife.actOf(e), MarkLife.whyOf(e), MarkLife.noteOf(e.extra), MarkLife.isOffice(e.extra)))
                    assertEquals(emptySet(), e.extra.keys intersect MarkLife.KEYS.toSet())
                    assertEquals("doors-windows", Trades.of(s, e))
                    // written with every default: placed as Existing, General, no note, an act where none belongs -- and to a wish and back
                    assertNull(MarkLife.placeAs(e, PhaseTools.asFor(PhaseTools.SURVEY), PhaseTools.SURVEY, at))
                    Trades.setCat(e, Trades.GENERAL); MarkLife.setNote(e, "  ", office = true); assertFalse(MarkLife.setAct(e, "shift"))
                    // ... nor a sub-kind (port 62 slice L5): none, the first, one nobody knows -- a v52 letter has none to store
                    for (sub in listOf(null, "switch", "zzz")) MarkTypes.setSub(s, e, sub)
                    assertTrue(MarkLife.move(e, "wish", PhaseTools.WISH_LIST, at, act = "shift").ok); assertTrue(MarkLife.move(e, "exist", PhaseTools.WISH_LIST, at).ok)
                }
                for (m in d.prep.surface(s).meas) { Trades.setCat(m, null); MarkLife.setNote(m, "", office = false) }
            }
            assertEquals("{exist=1, wish=0, vrev=0, vok=0, vadd=0, cok=0, scope=0, drop=0}", MarkLife.countLife(d.prep).toString())
            assertEquals(before, write(SurveyPrepFile.encode(d.prep, z[0], z[1], z[2])), "byte for byte the file it was")
        }
    }

    // ------------------------------------------------------------------ port 62 slice L5: the ten new types and their sub-kinds

    private val keys7 = listOf("id", "t", "code", "x0", "x1", "y0", "y1")

    @Test fun `a room marked with every new type and sub-kind by this build saves and reads back whole -- sub only when not the first`() {
        val l = 5458.5; val w = 3310.0; val h = 2705.0
        val p = SurveyPrep.fresh(l, w, h)
        // every new type on every surface it may go on, once per sub-kind (a Note, which has none, once); S's three on each wall
        val placed = LinkedHashMap<String, Triple<String, String, String?>>()   // code -> surface, token, the sub-kind it was placed as
        var i = 0
        for (s in SurveyPrep.SURFACES) {
            val ab = SurveyPrep.size(s, l, w, h)
            val types = MarkTypes.TOKENS.filter { MarkTypes.canGo(it, s) == true } + (if (SurveyPrep.isPlan(s)) emptyList() else listOf("S"))
            for (t in types) for (sub in MarkTypes.subsOf(t, s).map<MarkTypes.Sub, String?> { it.id }.ifEmpty { listOf(null) })
                p.addMark(s, t, ab[0], ab[1], "n${i++}", sub).let { placed[it.code] = Triple(s, t, sub) }
        }
        // 4 walls x (Note 1 + GRL 4 + AC 3 + PLB 6 + CTR 4 + CRP 8 + LV 5 + SAN 6 + DEF 5 + S 3) + floor (1 + 4 + 4 + 3 + 5) + ceiling (1 + 2 + 3 + 4 + 3 + 5)
        assertEquals(4 * 45 + 17 + 18, placed.size)
        // the last code of each type on a surface: numbered per TOKEN, the token whole
        fun last(s: String) = placed.entries.filter { it.value.first == s }.groupBy { it.value.second }.map { (_, rows) -> rows.last().key }
        assertEquals(listOf("FNOTE1", "FGRL4", "FAC3", "FPLB6", "FCTR4", "FCRP8", "FLV5", "FSAN6", "FDEF5", "FS3"), last("front"))
        assertEquals(listOf("LNOTE1", "LGRL4", "LAC3", "LPLB6", "LCTR4", "LCRP8", "LLV5", "LSAN6", "LDEF5", "LS3"), last("left"))
        assertEquals(listOf("GNOTE1", "GPLB4", "GCRP4", "GSAN3", "GDEF5"), last("floor"))
        assertEquals(listOf("CNOTE1", "CAC2", "CCDP3", "CCTR4", "CLV3", "CDEF5"), last("ceil"))
        // readings, under the keys the checklists ask for: a point's to its centre
        p.actual.putAll(mapOf("FPLB1.cl" to 325.0, "FPLB1.cs" to 900.0, "GPLB2.ck" to 410.0, "CLV3.cf" to 77.0, "FCRP2.p" to 600.0, "CCDP3.p" to 150.0, "FCTR1.w" to 1510.0, "FS2.w" to 300.0))

        val tree = SurveyPrepFile.encode(p, l, w, h); val text = write(tree)
        val o = open(text, Disk()).opened as SurveyPrepFile.Opened.Restored
        assertTrue(o.decoded.problems.isEmpty()); assertEquals(0, o.decoded.points, "this build's own points are 50 mm spots already"); assertFalse(SurveyPrepFile.saveOnOpen(o))
        val back = o.decoded.prep
        val again = SurveyPrepFile.encode(back, l, w, h)
        assertEquals(norm(tree), norm(again), "what was written is what is read and written again")
        assertEquals(p.actual, back.actual)
        var stored = 0
        for (s in SurveyPrep.SURFACES) {
            val els = elsOf(again, s); val marks = back.surface(s).marks
            assertEquals(elsOf(tree, s).map { it.keys.toList() }, els.map { it.keys.toList() }, "$s: every mark's keys, in the order they were written")
            assertEquals(els.size, marks.size, s)
            for ((j, e) in marks.withIndex()) {
                val (s0, t, sub) = placed.getValue(e.code); val first = MarkTypes.firstSub(t, s)
                assertEquals(s to t, s0 to e.t, e.code); assertEquals(t, MarkTypes.codeToken(e.code), e.code)
                // the file's row: the seven keys, and `sub` after them ONLY when it is not the type's first there
                assertEquals(if (sub == null || sub == first) keys7 else keys7 + "sub", els[j].keys.toList(), e.code)
                assertEquals(if (sub == null || sub == first) null else sub, els[j]["sub"], e.code)
                if (sub != null && sub != first) stored++
                // read back: the sub-kind it was placed as, its label, its trade, the box that sub-kind starts with
                assertEquals(sub, MarkTypes.subOf(s, e)?.id, e.code)
                val ab = SurveyPrep.size(s, l, w, h)
                assertEquals(SurveyPrep.defaults(s, t, ab[0], ab[1], sub).toList(), listOf(e.x0, e.x1, e.y0, e.y1), e.code)
                assertEquals(Trades.tradeOf(t, s, sub), Trades.of(s, e), e.code)
            }
        }
        // 4 walls x (GRL 3 + AC 2 + PLB 5 + CTR 3 + CRP 7 + LV 4 + SAN 5 + DEF 4 + S 2) + floor (3 + 3 + 2 + 4) + ceiling (1 + 2 + 3 + 2 + 4)
        assertEquals(4 * 35 + 12 + 12, stored)
        assertEquals("Kitchen base", MarkTypes.displayName(Tx.EN, "front", back.markOf("FCRP2.p")!!.second)); assertEquals("Full false ceiling", MarkTypes.displayName(Tx.EN, "ceil", back.markOf("CCDP3.p")!!.second))
        assertEquals("DB / MCB board", MarkTypes.displayName(Tx.EN, "front", back.markOf("FS2.w")!!.second)); assertEquals("Plumbing point · Waste outlet", MarkTypes.displayName(Tx.EN, "floor", back.markOf("GPLB2.ck")!!.second))
        // a kind picked later is one key changed on one mark, and nothing else in the file
        val e = back.markOf("FCRP2.p")!!.second
        MarkTypes.setSub("front", e, "tv")
        val after = SurveyPrepFile.encode(back, l, w, h)
        assertEquals("tv", elsOf(after, "front").first { it["code"] == "FCRP2" }["sub"])
        assertEquals(elsOf(tree, "front").filter { it["code"] != "FCRP2" }.map { norm(it) }, elsOf(after, "front").filter { it["code"] != "FCRP2" }.map { norm(it) })
        for (s in SurveyPrep.SURFACES - "front") assertEquals(elsOf(tree, s).map { norm(it) }, elsOf(after, s).map { norm(it) }, s)
        MarkTypes.setSub("front", e, "wardrobe"); assertEquals(keys7, elsOf(SurveyPrepFile.encode(back, l, w, h), "front").first { it["code"] == "FCRP2" }.keys.toList(), "back to the first: the key is gone")
    }

    @Test fun `a sub-kind, a token and a key this build does not know are kept as they came -- and an older box of a new point opens as a spot`() {
        // as a later build, or the prototype, may have written it: a CRP whose kind this build has no row for, a type nobody
        // knows yet, a Defect with keys beside its sub-kind, and a Plumbing point drawn 150 x 150 with a reading to its edge
        val added = listOf(
            """{"id":"u1","t":"CRP","code":"FCRP1","x0":150,"x1":1350,"y0":0,"y1":2100,"locked":false,"sub":"bar-unit","later":{"finish":"teak","n":[1,null]}}""",
            """{"id":"u2","t":"SKY","code":"FSKY1","x0":10,"x1":310,"y0":10,"y1":210,"sub":"dome","cat":"civil"}""",
            """{"id":"u3","t":"DEF","code":"FDEF1","x0":500,"x1":800,"y0":500,"y1":800,"sub":"tile","note":"three tiles hollow","vis":"office","photo":["a.jpg"]}""",
            """{"id":"u4","t":"PLB","code":"FPLB1","x0":300,"x1":450,"y0":800,"y1":950,"sub":"mixer"}""",
        ).joinToString(",")
        val text = v1.replaceFirst("""[{"id":"a1","t":"D"""", "[$added," + """{"id":"a1","t":"D"""")
            .replace(""""actual":{""", """"actual":{"FPLB1.l":300,"FPLB1.s":800,"FCRP1.p":600,""")
        val o = open(text, Disk()).opened as SurveyPrepFile.Opened.Restored
        assertTrue(o.decoded.problems.isEmpty(), o.decoded.problems.map { it(Tx.EN) }.toString())
        val p = o.decoded.prep; val z = o.decoded.size!!
        val f = p.surface("front").marks.associateBy { it.id }
        // read: an unknown sub-kind is the type's first for every reader, and is still there; the unknown type is the grey unknown
        val u1 = f.getValue("u1")
        assertEquals("bar-unit", Trades.subOf(u1.extra)); assertEquals("wardrobe", MarkTypes.subOf("front", u1)!!.id); assertEquals("Wardrobe", MarkTypes.displayName(Tx.EN, "front", u1))
        val u2 = f.getValue("u2")
        assertEquals("SKY", SurveyPrep.typeOf("front", "SKY").name); assertNull(MarkTypes.subOf("front", u2)); assertEquals("SKY", MarkTypes.displayName(Tx.EN, "front", u2)); assertEquals("general", Trades.of("front", u2))
        assertEquals("tiling", Trades.of("front", f.getValue("u3"))); assertEquals("Broken / hollow tile", MarkTypes.displayName(Tx.EN, "front", f.getValue("u3")))
        // the Plumbing point was made a 50 mm spot at its centre on opening, its edge readings going to the centre keys (as E's do), its kind kept
        assertEquals(1, o.decoded.points); assertTrue(SurveyPrepFile.saveOnOpen(o))
        val u4 = f.getValue("u4")
        assertEquals(listOf(350.0, 400.0, 850.0, 900.0), listOf(u4.x0, u4.x1, u4.y0, u4.y1)); assertEquals("mixer", Trades.subOf(u4.extra))
        assertEquals(375.0, p.actual["FPLB1.cl"]); assertEquals(875.0, p.actual["FPLB1.cs"]); assertNull(p.actual["FPLB1.l"]); assertEquals(600.0, p.actual["FCRP1.p"])
        // written: every key of the three others exactly as it came, in its own order
        val saved = SurveyPrepFile.encode(p, z[0], z[1], z[2]); val was = elsOf(parse(text), "front")
        for (j in 0..2) { assertEquals(norm(was[j]), norm(elsOf(saved, "front")[j]), "mark ${j + 1}"); assertEquals(was[j].keys.toList(), elsOf(saved, "front")[j].keys.toList()) }
        assertEquals("bar-unit", elsOf(saved, "front")[0]["sub"]); assertEquals("dome", elsOf(saved, "front")[1]["sub"]); assertEquals("SKY", elsOf(saved, "front")[1]["t"])
        // a copy for undo and a lock keep the unknown sub-kind; only PICKING a kind replaces it
        val q = p.copy(); val c1 = q.markById("u1")!!.second
        c1.extra = SurveyPrep.withKey(c1.extra, SurveyPrep.LOCKED, true)
        assertEquals("bar-unit", elsOf(SurveyPrepFile.encode(q, z[0], z[1], z[2]), "front")[0]["sub"])
        MarkTypes.setSub("front", c1, "kbase"); assertEquals("kbase", elsOf(SurveyPrepFile.encode(q, z[0], z[1], z[2]), "front")[0]["sub"])
        assertEquals(norm(mapOf("finish" to "teak", "n" to listOf(1, null))), norm(elsOf(SurveyPrepFile.encode(q, z[0], z[1], z[2]), "front")[0]["later"]))
        assertEquals("bar-unit", Trades.subOf(p.markById("u1")!!.second.extra), "the prep the copy was taken from is untouched")
        // and the next codes run on from the ones the file holds, token by token
        assertEquals(listOf("FCRP2", "FPLB2", "FDEF2", "FSKY2", "FD2"), listOf("CRP", "PLB", "DEF", "SKY", "D").map { p.nextCode("front", it) })
    }
}
