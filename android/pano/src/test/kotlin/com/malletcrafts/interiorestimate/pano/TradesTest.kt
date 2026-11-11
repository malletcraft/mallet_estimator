package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Lang
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The 13 trades and the one trade every mark belongs to (port 62 slice L1), against the approved prototype v61c.2
 * (index61c2.html, T53: TRADES 2379-2392, tradeOf 2487, SEES / OWN / ON 2493-2504, SERVICE_TRADE 2509-2521, moreRows
 * 2602), table by table and row by row. Every expected value is written out by hand, in English and in Marathi; the
 * rows of ownOn, seesOn, moreRows and tradeOf are what the prototype's own module answers for the same input.
 */
class TradesTest {

    private val KINDS = listOf("wall", "floor", "ceil")

    /** The prototype's 22 type tokens: v52's twelve, then v53's ten. */
    private val V52 = listOf("W", "D", "C", "B", "O", "N", "L", "E", "P", "S", "F", "X")
    private val V53 = listOf("NOTE", "GRL", "AC", "CDP", "PLB", "CTR", "CRP", "LV", "SAN", "DEF")

    /** The prototype's sub-kind ids (SUBS 2423-2466), by type and kind of surface: slice L5 ports the table; here they are tradeOf's inputs. */
    private val SUBS: Map<String, Map<String, List<String>>> = mapOf(
        "S" to mapOf("wall" to listOf("switch", "db", "meter")),
        "GRL" to mapOf("wall" to listOf("window", "balcony", "railing", "door")),
        "AC" to mapOf("wall" to listOf("split", "window-ac", "outdoor"), "ceil" to listOf("cassette", "duct")),
        "CDP" to mapOf("ceil" to listOf("bulkhead", "cove", "full")),
        "PLB" to mapOf("wall" to listOf("cold", "hot", "mixer", "wm", "ro", "waste"), "floor" to listOf("trap", "waste", "cold", "hot")),
        "CTR" to mapOf("wall" to listOf("track", "rod", "roller", "vertical"), "ceil" to listOf("track", "rod", "roller", "vertical")),
        "CRP" to mapOf("wall" to listOf("wardrobe", "kbase", "kwall", "tv", "study", "shoe", "vanity", "other"),
            "floor" to listOf("bed", "platform", "island", "other")),
        "LV" to mapOf("wall" to listOf("camera", "intercom", "data", "tv", "wifi"), "ceil" to listOf("camera", "wifi", "smoke")),
        "SAN" to mapOf("wall" to listOf("basin", "wc-wall", "shower", "geyser", "sink", "mirror"), "floor" to listOf("wc", "shower-area", "tub")),
        "DEF" to KINDS.associateWith { listOf("crack", "damp", "hollow", "peeling", "tile") },
    )

    private fun mark(t: String, extra: Map<String, Any?> = emptyMap()) = SurveyPrep.Mark("a", t, "F${t}1", 0.0, 50.0, 0.0, 50.0, extra)
    private fun meas(kind: String, extra: Map<String, Any?> = emptyMap()) =
        SurveyPrep.Meas("m", kind, "F-M1", mutableListOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(100.0, 0.0), doubleArrayOf(100.0, 100.0)), extra)

    @Test fun `the 13 trades, in the prototype's order, each with its id, its dot and its two names in both languages`() {
        assertEquals(listOf("civil", "doors-windows", "grills", "electrical", "lowvolt", "hvac", "false-ceiling", "carpentry", "plumbing",
            "tiling", "painting", "curtains", "general"), Trades.IDS)
        assertEquals((1..13).toList(), Trades.ALL.map { it.order })
        assertEquals(listOf(0xb197fc, 0x4dabf7, 0x82c91e, 0xffd43b, 0x7048e8, 0x99e9f2, 0x96f2d7, 0xa0522d, 0x15aabf, 0xff8787, 0x63e6be,
            0xc2255c, 0xadb5bd), Trades.ALL.map { it.dot })
        assertEquals(listOf("Civil & structure", "Doors & windows", "Grills & railings", "Electrical", "CCTV, intercom & data",
            "AC & ventilation", "False ceiling", "Carpentry & furniture", "Plumbing & sanitary", "Tiling", "Painting & finishes",
            "Curtains & blinds", "General"), Trades.IDS.map { Trades.name(Tx.EN, it) })
        assertEquals(listOf("Civil", "Doors & win.", "Grills", "Electrical", "CCTV & data", "AC", "False ceil.", "Carpentry", "Plumbing",
            "Tiling", "Painting", "Curtains", "General"), Trades.IDS.map { Trades.short(Tx.EN, it) })
        assertEquals(listOf("सिव्हिल व स्ट्रक्चर", "दरवाजे व खिडक्या", "ग्रिल व रेलिंग", "इलेक्ट्रिकल", "CCTV, इंटरकॉम व डेटा",
            "AC व व्हेंटिलेशन", "फॉल्स सीलिंग", "सुतारकाम व फर्निचर", "प्लंबिंग व सॅनिटरी", "टाइलिंग", "रंगकाम व फिनिश",
            "पडदे व ब्लाइंड्स", "सामान्य"), Trades.IDS.map { Trades.name(Tx.MR, it) })
        // AC is an abbreviation the prototype draws with no key and shows as written under मराठी too: trade.hvac.short holds it
        assertEquals(listOf("सिव्हिल", "दारे-खिडक्या", "ग्रिल", "इलेक्ट्रिकल", "CCTV व डेटा", "AC", "फॉ. सीलिंग", "सुतारकाम", "प्लंबिंग",
            "टाइलिंग", "रंगकाम", "पडदे", "सामान्य"), Trades.IDS.map { Trades.short(Tx.MR, it) })
        assertEquals("general", Trades.GENERAL); assertEquals(Trades.IDS.last(), Trades.GENERAL)
        assertEquals(13, Trades.IDS.toSet().size)
    }

    /**
     * The L1 review, finding 2: a trade's name and short name are SHOWN (a chip, a row label, the Mark sheet's head), so
     * each is read from the tables -- none is a literal in the code, however the same it reads in both languages. Asked
     * of a table whose every cell is a marker, each of the 26 must come back as one cell, whole.
     */
    @Test fun `every trade's name and short name is a cell of the tables, never a literal in the code`() {
        val tab = Tx::class.java.getDeclaredField("tab").apply { isAccessible = true }
        val cells = (tab.get(Tx.EN) as Array<*>).size
        val probe = Tx::class.java.getDeclaredConstructor(Lang::class.java, Array<String>::class.java).apply { isAccessible = true }
            .newInstance(Lang.EN, Array(cells) { "\u27e6$it\u27e7" })
        val cell = Regex("^\u27e6\\d+\u27e7$")
        for (id in Trades.IDS + listOf(null, "plaster")) {
            assertTrue(cell.matches(Trades.name(probe, id)), "name of $id: ${Trades.name(probe, id)}")
            assertTrue(cell.matches(Trades.short(probe, id)), "short of $id: ${Trades.short(probe, id)}")
        }
        // 13 names; 10 shorts of their own, and Electrical, Tiling and General short already: one key serves both
        assertEquals(13, Trades.IDS.map { Trades.name(probe, it) }.toSet().size)
        assertEquals(13, Trades.IDS.map { Trades.short(probe, it) }.toSet().size)
        assertEquals(listOf("electrical", "tiling", "general"), Trades.IDS.filter { Trades.name(probe, it) == Trades.short(probe, it) })
    }

    @Test fun `a trade by its id -- and an id outside the 13 is no trade, read as General`() {
        assertEquals(8, Trades.byId("carpentry")!!.order)
        for (bad in listOf(null, "", "Carpentry", "woodwork")) {
            assertNull(Trades.byId(bad), "$bad")
            assertEquals("general", Trades.known(bad), "$bad")
            assertEquals("General", Trades.name(Tx.EN, bad)); assertEquals("General", Trades.short(Tx.EN, bad))
            assertEquals("सामान्य", Trades.name(Tx.MR, bad))
        }
        for (id in Trades.IDS) assertEquals(id, Trades.known(id))
    }

    @Test fun `tradeOf - every type on every kind of surface, with every sub-kind`() {
        // every type has ONE trade on every surface, whatever its sub-kind -- except S and DEF, pinned below
        val one = mapOf("W" to "doors-windows", "D" to "doors-windows", "C" to "civil", "B" to "civil", "O" to "civil", "N" to "civil",
            "L" to "carpentry", "E" to "electrical", "P" to "electrical", "F" to "electrical", "X" to "general", "NOTE" to "general",
            "GRL" to "grills", "AC" to "hvac", "CDP" to "false-ceiling", "PLB" to "plumbing", "CTR" to "curtains", "CRP" to "carpentry",
            "LV" to "lowvolt", "SAN" to "plumbing")
        assertEquals((V52 + V53).toSet() - setOf("S", "DEF"), one.keys)
        for (k in KINDS) for ((t, trade) in one) {
            assertEquals(trade, Trades.tradeOf(t, k), "$t on $k")
            for (sub in SUBS[t]?.get(k).orEmpty()) assertEquals(trade, Trades.tradeOf(t, k, sub), "$t · $sub on $k")
        }
        // S: the floor's Step is Civil's; a Switch plate, DB or meter board Electrical's -- on a wall and (as the prototype answers) on the ceiling
        assertEquals("civil", Trades.tradeOf("S", "floor"))
        assertEquals("electrical", Trades.tradeOf("S", "wall")); assertEquals("electrical", Trades.tradeOf("S", "ceil"))
        for (sub in SUBS.getValue("S").getValue("wall")) assertEquals("electrical", Trades.tradeOf("S", "wall", sub), sub)
        // DEF: Broken / hollow tile is Tiling's, every other Defect Painting's -- and a Defect with no sub-kind stored is the first, a Crack
        for (k in KINDS) {
            assertEquals("painting", Trades.tradeOf("DEF", k), k)
            assertEquals(listOf("painting", "painting", "painting", "painting", "tiling"), SUBS.getValue("DEF").getValue(k).map { Trades.tradeOf("DEF", k, it) }, k)
        }
    }

    @Test fun `tradeOf reads a surface as its kind -- the four walls are one kind`() {
        assertEquals(listOf("wall", "wall", "wall", "wall", "floor", "ceil"), SurveyPrep.SURFACES.map { Trades.kindOf(it) })
        assertEquals(SurveyPrep.SURFACES.map { SurveyPrep.kind(it) }, SurveyPrep.SURFACES.map { Trades.kindOf(it) }, "the phone's own kind rule")
        assertEquals(listOf("wall", "floor", "ceil"), KINDS.map { Trades.kindOf(it) })
        assertEquals(listOf("electrical", "electrical", "electrical", "electrical", "civil", "electrical"), SurveyPrep.SURFACES.map { Trades.tradeOf("S", it) })
    }

    @Test fun `a Note, an Other, a Line and an area take their stored cat -- General by default and for a cat that is no trade`() {
        for (t in listOf("NOTE", "X", "line", "area")) {
            assertEquals("carpentry", Trades.tradeOf(t, "wall", null, "carpentry"), t)
            assertEquals("general", Trades.tradeOf(t, "wall", null, "general"), t)
            assertEquals("general", Trades.tradeOf(t, "wall", null, "nonsense"), t)
            assertEquals("general", Trades.tradeOf(t, "wall"), t)
            for (id in Trades.IDS) assertEquals(id, Trades.tradeOf(t, "floor", null, id), "$t · $id")
        }
        // every other type ignores a cat it should never carry
        assertEquals("doors-windows", Trades.tradeOf("W", "wall", null, "carpentry"))
        assertEquals("line", SurveyMeas.LINE); assertEquals("area", SurveyMeas.AREA)
    }

    @Test fun `a type this build does not know is General`() {
        assertEquals("general", Trades.tradeOf("ZZ", "wall"))
        assertEquals("general", Trades.tradeOf("", "ceil", "tile", "carpentry"))
        assertEquals("general", Trades.tradeOf("tape", "wall", null, "carpentry"), "a Tape has no cat: of() answers null for it")
    }

    @Test fun `the trade of a mark, a Line and an area is read from its own keys -- a Tape has none`() {
        assertEquals("doors-windows", Trades.of("front", mark("W")))
        assertEquals("civil", Trades.of("floor", mark("S"))); assertEquals("electrical", Trades.of("left", mark("S")))
        assertEquals("tiling", Trades.of("floor", mark("DEF", mapOf("sub" to "tile")))); assertEquals("painting", Trades.of("floor", mark("DEF", mapOf("sub" to "damp"))))
        assertEquals("plumbing", Trades.of("front", mark("X", mapOf("cat" to "plumbing")))); assertEquals("general", Trades.of("front", mark("X")))
        assertEquals("general", Trades.of("front", mark("NOTE", mapOf("cat" to 7))), "a cat that is no string is no trade")
        assertEquals("painting", Trades.of("ceil", meas("area", mapOf("cat" to "painting")))); assertEquals("general", Trades.of("ceil", meas("line")))
        assertNull(Trades.of("front", meas("tape", mapOf("cat" to "painting"))))
        assertEquals("sub", Trades.KEY_SUB); assertEquals("cat", Trades.KEY_CAT)
        assertEquals("wardrobe", Trades.subOf(mapOf("sub" to "wardrobe"))); assertNull(Trades.subOf(mapOf("sub" to 1))); assertNull(Trades.subOf(emptyMap()))
        assertEquals("civil", Trades.catOf(mapOf("cat" to "civil"))); assertNull(Trades.catOf(emptyMap()))
    }

    @Test fun `TRADE is offered for an Other, a Note, a Line and an area -- never a Tape or another type`() {
        assertEquals(listOf("X", "NOTE"), (V52 + V53).filter { Trades.takesCat(mark(it)) })
        assertEquals(listOf(false, true, true), listOf("tape", "line", "area").map { Trades.takesCat(meas(it)) })
        assertEquals("NOTE", Trades.NOTE); assertEquals("X", Trades.OTHER)
    }

    @Test fun `a trade is stored only when it is not General -- in a new map, every other key where it was`() {
        val before: Map<String, Any?> = linkedMapOf("locked" to true, "future" to mapOf("a" to listOf(1, 2)), "note" to "kitchen")
        val set = Trades.withCat(before, "carpentry")
        assertEquals(listOf("locked", "future", "note", "cat"), set.keys.toList()); assertEquals("carpentry", set["cat"])
        assertEquals(listOf("locked", "future", "note"), before.keys.toList(), "the old map is not changed: an undo copy may share it")
        assertNotSame(before, set)
        // back to General, or to none: the key is DELETED, so the mark is the one it was
        assertEquals(before, Trades.withCat(set, "general")); assertEquals(before.keys.toList(), Trades.withCat(set, "general").keys.toList())
        assertEquals(before, Trades.withCat(set, null))
        assertEquals(before, Trades.withCat(before, "general"), "General on a mark with no cat writes nothing")
        // another trade over one keeps the key's place
        assertEquals(listOf("cat", "note"), Trades.withCat(linkedMapOf("cat" to "civil", "note" to "x"), "tiling").keys.toList())
        val e = mark("X", before); val undo = e.copy()
        Trades.setCat(e, "plumbing")
        assertEquals("plumbing", Trades.of("front", e)); assertEquals("general", Trades.of("front", undo))
        val m = meas("line"); Trades.setCat(m, "painting"); assertEquals(mapOf("cat" to "painting"), m.extra)
        Trades.setCat(m, "general"); assertEquals(emptyMap(), m.extra)
    }

    @Test fun `SEES - what each trade sees as grey context, row by row`() {
        assertEquals(mapOf(
            "civil" to "W D",
            "doors-windows" to "C B O N GRL L",
            "grills" to "W D O C B",
            "electrical" to "W D CRP SAN PLB AC C B L",
            "lowvolt" to "W D E S CRP C B",
            "hvac" to "W D E S CRP C B CDP CTR",
            "false-ceiling" to "B C O E P F AC LV CTR",
            "carpentry" to "W D L N S E PLB C B O AC",
            "plumbing" to "W D CRP E S C B",
            "tiling" to "W D PLB SAN E S C B",
            "painting" to "W D O N L C B GRL CRP S",
            "curtains" to "W D O C B F AC",
            "general" to "",
        ), Trades.SEES.mapValues { it.value.joinToString(" ") })
        assertEquals(Trades.IDS, Trades.SEES.keys.toList())
        // S is seen as the Switch plate: on walls only
        assertTrue(Trades.sees("carpentry", "S", "wall")); assertTrue(Trades.sees("carpentry", "S", "back"))
        assertFalse(Trades.sees("carpentry", "S", "floor")); assertFalse(Trades.sees("carpentry", "S", "ceil"))
        assertTrue(Trades.sees("civil", "W", "floor"), "sees() is the table alone: where a type goes is seesOn's")
        assertFalse(Trades.sees("civil", "C", "wall")); assertFalse(Trades.sees("general", "W", "wall")); assertFalse(Trades.sees("nobody", "W", "wall"))
    }

    @Test fun `OWN - each trade's own types in its own order, and ON - the types each kind of surface carries`() {
        assertEquals(mapOf(
            "civil" to "C B O N S@floor",
            "doors-windows" to "W D",
            "grills" to "GRL",
            "electrical" to "E P S@wall F",
            "lowvolt" to "LV",
            "hvac" to "AC",
            "false-ceiling" to "CDP",
            "carpentry" to "L CRP",
            "plumbing" to "PLB SAN",
            "tiling" to "",
            "painting" to "DEF",
            "curtains" to "CTR",
            "general" to "NOTE X",
        ), Trades.OWN.mapValues { it.value.joinToString(" ") })
        assertEquals(Trades.IDS, Trades.OWN.keys.toList())
        assertEquals("W D C B O N L E P S F X NOTE GRL AC PLB CTR CRP LV SAN DEF", Trades.ON.getValue("wall").joinToString(" "))
        assertEquals("C S O E P X NOTE PLB CRP SAN DEF", Trades.ON.getValue("floor").joinToString(" "))
        assertEquals("B C E P F O X NOTE AC CDP CTR LV DEF", Trades.ON.getValue("ceil").joinToString(" "))
        assertEquals(listOf(21, 11, 13), KINDS.map { Trades.ON.getValue(it).size })
        assertEquals(KINDS, Trades.ON.keys.toList())
        // v52's part is the phone's own type list, in its own order; v53's is the ten, in their order, where each may go
        for (k in KINDS) {
            val v52 = SurveyPrep.TYPES.getValue(k).map { it.letter }
            assertEquals(v52, Trades.ON.getValue(k).take(v52.size), k)
            assertEquals(Trades.ON.getValue(k).drop(v52.size), V53.filter { it in Trades.ON.getValue(k) }, k)
        }
        assertEquals((V52 + V53).toSet(), Trades.ON.values.flatten().toSet())
        // every type but the two letters with two meanings is exactly one trade's own, and it is the trade tradeOf answers
        val owners = Trades.OWN.flatMap { (trade, own) -> own.map { it.substringBefore('@') to trade } }
        assertEquals((V52 + V53).toSet(), owners.map { it.first }.toSet())
        for ((t, trade) in owners) if (t != "S" && t != "DEF") assertEquals(trade, Trades.tradeOf(t, "wall"), t)
        assertEquals("painting", Trades.OWN.entries.single { "DEF" in it.value }.key)
    }

    @Test fun `ownOn and seesOn - every trade on every kind of surface`() {
        fun row(k: String, trade: String) = Trades.ownOn(trade, k).joinToString(" ") + " | " + Trades.seesOn(trade, k).joinToString(" ")
        assertEquals(listOf(
            "C B O N | W D",
            "W D | C B O N GRL L",
            "GRL | W D O C B",
            "E P S F | W D CRP SAN PLB AC C B L",
            "LV | W D E S CRP C B",
            "AC | W D E S CRP C B CTR",
            " | B C O E P F AC LV CTR",
            "L CRP | W D L N S E PLB C B O AC",
            "PLB SAN | W D CRP E S C B",
            " | W D PLB SAN E S C B",
            "DEF | W D O N L C B GRL CRP S",
            "CTR | W D O C B F AC",
            "NOTE X | ",
        ), Trades.IDS.map { row("wall", it) })
        assertEquals(listOf(
            "C O S | ",
            " | C O",
            " | O C",
            "E P | CRP SAN PLB C",
            " | E CRP C",
            " | E CRP C",
            " | C O E P",
            "CRP | E PLB C O",
            "PLB SAN | CRP E C",
            " | PLB SAN E C",
            "DEF | O C CRP",
            " | O C",
            "NOTE X | ",
        ), Trades.IDS.map { row("floor", it) })
        assertEquals(listOf(
            "C B O | ",
            " | C B O",
            " | O C B",
            "E P F | AC C B",
            "LV | E C B",
            "AC | E C B CDP CTR",
            "CDP | B C O E P F AC LV CTR",
            " | E C B O AC",
            " | E C B",
            " | E C B",
            "DEF | O C B",
            "CTR | O C B F AC",
            "NOTE X | ",
        ), Trades.IDS.map { row("ceil", it) })
        // a surface id reads as its kind; a trade that is none owns and sees nothing
        assertEquals(listOf("C", "B", "O", "N"), Trades.ownOn("civil", "right")); assertEquals(listOf("W", "D", "E", "S", "CRP", "C", "B"), Trades.seesOn("lowvolt", "left"))
        assertEquals(emptyList(), Trades.ownOn("nobody", "wall")); assertEquals(emptyList(), Trades.seesOn("nobody", "wall"))
    }

    @Test fun `More - one row per trade with a type on that surface, General first, a picked trade's row first of all`() {
        fun rows(k: String, picked: String? = null) = Trades.moreRows(k, picked).map { (if (it.picked) "*" else "") + it.trade + ":" + it.types.joinToString(" ") }
        assertEquals(listOf("general:NOTE X", "civil:C B O N", "doors-windows:W D", "grills:GRL", "electrical:E P S F", "lowvolt:LV", "hvac:AC",
            "carpentry:L CRP", "plumbing:PLB SAN", "painting:DEF", "curtains:CTR"), rows("wall"))
        assertEquals(listOf("general:NOTE X", "civil:C O S", "electrical:E P", "carpentry:CRP", "plumbing:PLB SAN", "painting:DEF"), rows("floor"))
        assertEquals(listOf("general:NOTE X", "civil:C B O", "electrical:E P F", "lowvolt:LV", "hvac:AC", "false-ceiling:CDP", "painting:DEF",
            "curtains:CTR"), rows("ceil"))
        // every type of a surface is on exactly one row
        for (k in KINDS) assertEquals(Trades.ON.getValue(k).sorted(), Trades.moreRows(k).flatMap { it.types }.sorted(), k)
        // picked: its row moves to the top, the rest keep their order
        assertEquals(listOf("*carpentry:L CRP", "general:NOTE X", "civil:C B O N", "doors-windows:W D", "grills:GRL", "electrical:E P S F", "lowvolt:LV",
            "hvac:AC", "plumbing:PLB SAN", "painting:DEF", "curtains:CTR"), rows("wall", "carpentry"))
        assertEquals(listOf("*carpentry:CRP", "general:NOTE X", "civil:C O S", "electrical:E P", "plumbing:PLB SAN", "painting:DEF"), rows("floor", "carpentry"))
        // a picked trade with no type on that surface has no row, so nothing moves; General picked is first already
        assertEquals(rows("ceil"), rows("ceil", "carpentry"))
        assertEquals(listOf("*general:NOTE X") + rows("wall").drop(1), rows("wall", "general"))
        assertEquals(rows("wall"), rows("front"), "a surface id reads as its kind")
    }

    @Test fun `SERVICE_TRADE - the trades that answer each of the wish list's twelve services, in S5's order`() {
        assertEquals(WishList.SERVICES.map { it.id }, Trades.SERVICE_TRADE.keys.toList())
        assertEquals(mapOf(
            "electrical" to "electrical hvac",
            "cctv" to "lowvolt",
            "intercom" to "lowvolt",
            "false-ceiling" to "false-ceiling",
            "doors-windows" to "doors-windows",
            "balcony-grill" to "grills",
            "carpentry" to "carpentry",
            "master-bath" to "plumbing tiling",
            "guest-bath" to "plumbing tiling",
            "painting" to "painting",
            "curtains" to "curtains",
            "plumbing" to "plumbing",
        ), Trades.SERVICE_TRADE.mapValues { it.value.trades.joinToString(" ") })
        // only the two low-voltage services name sub-kinds; only the two bathrooms name a room
        assertEquals(mapOf("cctv" to listOf("camera", "data", "tv", "wifi", "smoke"), "intercom" to listOf("intercom")),
            Trades.SERVICE_TRADE.filterValues { it.subs != null }.mapValues { it.value.subs })
        assertEquals(mapOf("master-bath" to "master", "guest-bath" to "guest"), Trades.SERVICE_TRADE.filterValues { it.room != null }.mapValues { it.value.room })
        // between them the two low-voltage services name every Low-voltage sub-kind of the prototype, each once
        val lv = SUBS.getValue("LV").values.flatten().toSet()
        assertEquals(lv, Trades.SERVICE_TRADE.values.flatMap { it.subs.orEmpty() }.toSet())
        assertEquals(lv.size, Trades.SERVICE_TRADE.values.sumOf { it.subs.orEmpty().size })
        for (s in Trades.SERVICE_TRADE.values) for (t in s.trades) assertTrue(t in Trades.IDS, t)
    }
}
