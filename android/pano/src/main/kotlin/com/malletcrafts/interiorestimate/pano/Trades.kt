package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * THE 13 TRADES, AND THE ONE TRADE EVERY MARK BELONGS TO (port 62 slice L1, 11 Nov 2026; the approved prototype
 * v61c.2, index61c2.html, module T53: TRADES 2379-2392, tradeOf 2487, SEES / OWN / ON 2493-2504, SERVICE_TRADE
 * 2509-2521, moreRows 2602). Amit, 16 Oct 2026, 06:37 IST: "Make mark type categories better by service vendor
 * point of view." / "electrician is interested in electrical only, window guy in door and windows".
 *
 * Pure data and pure functions, ported whole and pinned row by row in TradesTest. NOTHING ON SCREEN READS THIS YET:
 * the Mark sheet's trade dot and its TRADE picker are slice L3's, More ▸ by trade L6's, the wish list's counts L9's.
 * The tables name all 22 types of the prototype -- v52's twelve letters, which the phone has, and the ten
 * abbreviations NOTE GRL AC CDP PLB CTR CRP LV SAN DEF, which [SurveyPrep.TYPES] gains in L5 -- because a prep the
 * prototype wrote already carries them and every one has a trade.
 *
 * A trade's ID is the stored value (a Note's, an Other's, a Line's or an area's `cat`) and never changes; its two
 * names are shown through [name] and [short]. Not ported, by the plan's listed subset (DESIGN-port62 §7, Q-list 3):
 * the Trade chip and its MarkFilter, and with them the palette's three slots by trade.
 */
object Trades {

    const val CIVIL = "civil"
    const val DOORS_WINDOWS = "doors-windows"
    const val GRILLS = "grills"
    const val ELECTRICAL = "electrical"
    const val LOWVOLT = "lowvolt"
    const val HVAC = "hvac"
    const val FALSE_CEILING = "false-ceiling"
    const val CARPENTRY = "carpentry"
    const val PLUMBING = "plumbing"
    const val TILING = "tiling"
    const val PAINTING = "painting"
    const val CURTAINS = "curtains"
    const val GENERAL = "general"

    /** One trade: its stored [id], its dot's colour (0xRRGGBB) and its place in the order used everywhere (1..13). */
    class Trade(val id: String, val dot: Int, val order: Int)

    /** The 13, in the order every picker, row and list uses (TRADES; General last). */
    val ALL: List<Trade> = listOf(
        CIVIL to 0xb197fc, DOORS_WINDOWS to 0x4dabf7, GRILLS to 0x82c91e, ELECTRICAL to 0xffd43b, LOWVOLT to 0x7048e8,
        HVAC to 0x99e9f2, FALSE_CEILING to 0x96f2d7, CARPENTRY to 0xa0522d, PLUMBING to 0x15aabf, TILING to 0xff8787,
        PAINTING to 0x63e6be, CURTAINS to 0xc2255c, GENERAL to 0xadb5bd,
    ).mapIndexed { i, (id, dot) -> Trade(id, dot, i + 1) }

    val IDS: List<String> = ALL.map { it.id }

    /** The trade [id] names, or null when it is not one of the 13. */
    fun byId(id: String?): Trade? = ALL.firstOrNull { it.id == id }

    /** [id] when it is one of the 13, else General: an unknown or absent `cat` reads General (tradeOf, `TRADE[cat]?cat:'general'`). */
    fun known(id: String?): String = if (byId(id) != null) id!! else GENERAL

    /** A trade's full name ("Carpentry & furniture"); an id outside the 13 reads General's, as [known]. */
    fun name(tx: Tx, id: String?): String = when (known(id)) {
        CIVIL -> tx.trade_civil
        DOORS_WINDOWS -> tx.trade_doors_windows
        GRILLS -> tx.trade_grills
        ELECTRICAL -> tx.trade_electrical
        LOWVOLT -> tx.trade_lowvolt
        HVAC -> tx.trade_hvac
        FALSE_CEILING -> tx.trade_false_ceiling
        CARPENTRY -> tx.trade_carpentry
        PLUMBING -> tx.trade_plumbing
        TILING -> tx.trade_tiling
        PAINTING -> tx.trade_painting
        CURTAINS -> tx.trade_curtains
        else -> tx.trade_general
    }

    /**
     * A trade's short name, as a chip, a row label and the Mark sheet's head print it ("Doors & win.", "CCTV & data").
     * Electrical, Tiling and General are short already, so their one key serves both; AC & ventilation's short is the
     * abbreviation AC, which the prototype has no key for and shows as written in both languages.
     */
    fun short(tx: Tx, id: String?): String = when (known(id)) {
        CIVIL -> tx.trade_civil_short
        DOORS_WINDOWS -> tx.trade_doors_windows_short
        GRILLS -> tx.trade_grills_short
        ELECTRICAL -> tx.trade_electrical
        LOWVOLT -> tx.trade_lowvolt_short
        HVAC -> "AC" // text:value the prototype's short for AC & ventilation: an abbreviation with no key, shown as written (latin-ok.txt)
        FALSE_CEILING -> tx.trade_false_ceiling_short
        CARPENTRY -> tx.trade_carpentry_short
        PLUMBING -> tx.trade_plumbing_short
        TILING -> tx.trade_tiling
        PAINTING -> tx.trade_painting_short
        CURTAINS -> tx.trade_curtains_short
        else -> tx.trade_general
    }

    // ------------------------------------------------------------------ tradeOf

    /** The Note's type token (decision 1 of 16 Oct 2026: its codes are FNOTE1, GNOTE2, CNOTE1 ...). */
    const val NOTE = "NOTE"

    /** The Other type: with a Note, a Line and an area, the items whose trade is their stored [KEY_CAT]. */
    const val OTHER = "X"

    /** The key a Note, an Other, a Line or an area keeps its trade under; never written for General. */
    const val KEY_CAT = "cat"

    /** The key a mark keeps its sub-kind under (slice L5's; here only what [tradeOf] reads of it); never written for the first. */
    const val KEY_SUB = "sub"

    /** The Defect sub-kind whose trade is Tiling ("Broken / hollow tile"); every other Defect is Painting's. */
    const val SUB_TILE = "tile"

    /** The types with ONE trade on every surface (FIXED). S, DEF, NOTE and X are not here: [tradeOf] decides them. */
    val FIXED: Map<String, String> = linkedMapOf(
        "W" to DOORS_WINDOWS, "D" to DOORS_WINDOWS, "C" to CIVIL, "B" to CIVIL, "O" to CIVIL, "N" to CIVIL, "L" to CARPENTRY,
        "E" to ELECTRICAL, "P" to ELECTRICAL, "F" to ELECTRICAL,
        "GRL" to GRILLS, "AC" to HVAC, "CDP" to FALSE_CEILING, "PLB" to PLUMBING, "CTR" to CURTAINS, "CRP" to CARPENTRY,
        "LV" to LOWVOLT, "SAN" to PLUMBING,
    )

    /** A surface id (front, right, back, left, floor, ceil) or a kind, as its kind: floor, ceil, else wall (kindOf). */
    fun kindOf(s: String): String = if (s == "floor" || s == "ceil") s else "wall"

    /**
     * The trade of a type [t] on surface (or kind) [s], with its sub-kind [sub] and stored [cat]: every type has ONE
     * trade on every surface, except S (the floor's Step is Civil; a Switch plate anywhere else Electrical); a Defect
     * takes it from its sub-kind (Broken / hollow tile: Tiling; every other: Painting); a Note, an Other, a Line and
     * an area ([t] = `line` | `area`, [SurveyMeas.LINE] / [SurveyMeas.AREA]) take their stored cat, General by default
     * and for a cat that is not one of the 13. A type this build does not know is General.
     */
    fun tradeOf(t: String, s: String, sub: String? = null, cat: String? = null): String {
        if (t == "S") return if (kindOf(s) == "floor") CIVIL else ELECTRICAL
        if (t == "DEF") return if (sub == SUB_TILE) TILING else PAINTING
        if (t == NOTE || t == OTHER || t == SurveyMeas.LINE || t == SurveyMeas.AREA) return known(cat)
        return FIXED[t] ?: GENERAL
    }

    /** A mark's stored cat, as it came (null when absent or not a string); [tradeOf] reads an unknown one as General. */
    fun catOf(extra: Map<String, Any?>): String? = extra[KEY_CAT] as? String

    /** A mark's stored sub-kind, as it came (null when absent or not a string). */
    fun subOf(extra: Map<String, Any?>): String? = extra[KEY_SUB] as? String

    /** The trade of the mark [e] on surface [s]. */
    fun of(s: String, e: SurveyPrep.Mark): String = tradeOf(e.t, s, subOf(e.extra), catOf(e.extra))

    /**
     * The trade of a Line or an area on surface [s], by its stored cat; null for a Tape, which carries no trade (it
     * is the surveyor's own measurement). The prototype's MarkFilter.tradeOfItem, minus the chain, which is its own
     * class here and has none either.
     */
    fun of(s: String, m: SurveyPrep.Meas): String? = if (m.kind == SurveyMeas.TAPE) null else tradeOf(m.kind, s, null, catOf(m.extra))

    /** Whether the Mark sheet offers TRADE for the mark: an Other or a Note (takesCat). A Line and an area always do. */
    fun takesCat(e: SurveyPrep.Mark): Boolean = e.t == OTHER || e.t == NOTE

    /** ... and for a Tape, a Line or an area: a Line and an area, never a Tape (isLineArea). */
    fun takesCat(m: SurveyPrep.Meas): Boolean = m.kind == SurveyMeas.LINE || m.kind == SurveyMeas.AREA

    /**
     * [extra] with the trade [cat] as the Mark sheet's Done stores it: General, or none, DELETES the key -- a default
     * is never written, so a v52 file saves byte-identical -- any other is written. A NEW map: the old one may be
     * shared by an undo copy.
     */
    fun withCat(extra: Map<String, Any?>, cat: String?): Map<String, Any?> =
        LinkedHashMap(extra).apply { if (cat == null || cat == GENERAL) remove(KEY_CAT) else put(KEY_CAT, cat) }

    /** Gives the mark [e] the trade [cat] ([withCat]); the sheet offers it only where [takesCat]. */
    fun setCat(e: SurveyPrep.Mark, cat: String?) { e.extra = withCat(e.extra, cat) }

    /** Gives the Line or area [m] the trade [cat] ([withCat]). */
    fun setCat(m: SurveyPrep.Meas, cat: String?) { m.extra = withCat(m.extra, cat) }

    // ------------------------------------------------------------------ what a trade sees, and owns

    /** What each trade SEES as grey context (codes, no figures). S here is the Switch plate: it is seen on walls only ([sees]). */
    val SEES: Map<String, List<String>> = linkedMapOf(
        CIVIL to listOf("W", "D"),
        DOORS_WINDOWS to listOf("C", "B", "O", "N", "GRL", "L"),
        GRILLS to listOf("W", "D", "O", "C", "B"),
        ELECTRICAL to listOf("W", "D", "CRP", "SAN", "PLB", "AC", "C", "B", "L"),
        LOWVOLT to listOf("W", "D", "E", "S", "CRP", "C", "B"),
        HVAC to listOf("W", "D", "E", "S", "CRP", "C", "B", "CDP", "CTR"),
        FALSE_CEILING to listOf("B", "C", "O", "E", "P", "F", "AC", "LV", "CTR"),
        CARPENTRY to listOf("W", "D", "L", "N", "S", "E", "PLB", "C", "B", "O", "AC"),
        PLUMBING to listOf("W", "D", "CRP", "E", "S", "C", "B"),
        TILING to listOf("W", "D", "PLB", "SAN", "E", "S", "C", "B"),
        PAINTING to listOf("W", "D", "O", "N", "L", "C", "B", "GRL", "CRP", "S"),
        CURTAINS to listOf("W", "D", "O", "C", "B", "F", "AC"),
        GENERAL to emptyList(),
    )

    /** Whether [trade] sees type [t] on surface (or kind) [s] as context. */
    fun sees(trade: String, t: String, s: String): Boolean =
        SEES[trade]?.contains(t) == true && !(t == "S" && kindOf(s) != "wall")

    /**
     * A trade's OWN types, in its own order: More ▸'s rows follow it. `S@floor` and `S@wall` are the one letter that
     * is two types -- the floor's Step is Civil's, the wall's Switch plate Electrical's.
     */
    val OWN: Map<String, List<String>> = linkedMapOf(
        CIVIL to listOf("C", "B", "O", "N", "S@floor"),
        DOORS_WINDOWS to listOf("W", "D"),
        GRILLS to listOf("GRL"),
        ELECTRICAL to listOf("E", "P", "S@wall", "F"),
        LOWVOLT to listOf("LV"),
        HVAC to listOf("AC"),
        FALSE_CEILING to listOf("CDP"),
        CARPENTRY to listOf("L", "CRP"),
        PLUMBING to listOf("PLB", "SAN"),
        TILING to emptyList(),
        PAINTING to listOf("DEF"),
        CURTAINS to listOf("CTR"),
        GENERAL to listOf(NOTE, OTHER),
    )

    /**
     * The types each kind of surface carries: v52's first, in [SurveyPrep.TYPES]' own order (TradesTest holds the two
     * equal), then v53's ten where they may go -- 21 on a wall, 11 on the floor, 13 on the ceiling.
     */
    val ON: Map<String, List<String>> = linkedMapOf(
        "wall" to listOf("W", "D", "C", "B", "O", "N", "L", "E", "P", "S", "F", "X", "NOTE", "GRL", "AC", "PLB", "CTR", "CRP", "LV", "SAN", "DEF"),
        "floor" to listOf("C", "S", "O", "E", "P", "X", "NOTE", "PLB", "CRP", "SAN", "DEF"),
        "ceil" to listOf("B", "C", "E", "P", "F", "O", "X", "NOTE", "AC", "CDP", "CTR", "LV", "DEF"),
    )

    /** [trade]'s own types that go on surface (or kind) [s], in its own order (ownOn). */
    fun ownOn(trade: String, s: String): List<String> {
        val k = kindOf(s); val on = ON.getValue(k)
        return (OWN[trade] ?: emptyList()).mapNotNull { x ->
            val t = x.substringBefore('@'); val at = x.substringAfter('@', "")
            t.takeIf { it in on && (at.isEmpty() || at == k) }
        }
    }

    /** The types [trade] sees as context on surface (or kind) [s], in [SEES]' order (seesOn). */
    fun seesOn(trade: String, s: String): List<String> {
        val on = ON.getValue(kindOf(s))
        return (SEES[trade] ?: emptyList()).filter { it in on && sees(trade, it, s) }
    }

    /** One row of More ▸: a trade and its own types on that surface; [picked] when it is the trade asked for first. */
    data class Row(val trade: String, val types: List<String>, val picked: Boolean)

    /**
     * More ▸ as one row per trade with a type on surface (or kind) [s]: General first (Note is the first tile), then
     * the trades in their order; a [picked] trade's row first of all. With the Trade chip off the phone (Q-list 3)
     * nothing picks one, so [picked] is null there.
     */
    fun moreRows(s: String, picked: String? = null): List<Row> {
        val rows = (listOf(GENERAL) + IDS.filter { it != GENERAL })
            .map { Row(it, ownOn(it, s), it == picked) }.filter { it.types.isNotEmpty() }
        val i = rows.indexOfFirst { it.picked }
        return if (i > 0) listOf(rows[i]) + rows.subList(0, i) + rows.subList(i + 1, rows.size) else rows
    }

    // ------------------------------------------------------------------ the wish list's services

    /**
     * Which trades answer one of the wish list's twelve services ([WishList.SERVICES], by id). [subs]: only those
     * sub-kinds of a Low-voltage point; [room]: only marks in a room whose name matches (the two bathrooms).
     */
    class Service(val trades: List<String>, val subs: List<String>? = null, val room: String? = null)

    /** Wish-list service -> the trade(s) that answer it (SERVICE_TRADE), in S5's order of the twelve. Slice L9 counts with it. */
    val SERVICE_TRADE: Map<String, Service> = linkedMapOf(
        "electrical" to Service(listOf(ELECTRICAL, HVAC)),
        "cctv" to Service(listOf(LOWVOLT), subs = listOf("camera", "data", "tv", "wifi", "smoke")),
        "intercom" to Service(listOf(LOWVOLT), subs = listOf("intercom")),
        "false-ceiling" to Service(listOf(FALSE_CEILING)),
        "doors-windows" to Service(listOf(DOORS_WINDOWS)),
        "balcony-grill" to Service(listOf(GRILLS)),
        "carpentry" to Service(listOf(CARPENTRY)),
        "master-bath" to Service(listOf(PLUMBING, TILING), room = "master"),
        "guest-bath" to Service(listOf(PLUMBING, TILING), room = "guest"),
        "painting" to Service(listOf(PAINTING)),
        "curtains" to Service(listOf(CURTAINS)),
        "plumbing" to Service(listOf(PLUMBING)),
    )
}
