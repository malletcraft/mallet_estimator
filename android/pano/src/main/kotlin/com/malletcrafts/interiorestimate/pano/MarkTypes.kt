package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Masters
import com.malletcrafts.interiorestimate.pano.i18n.Msg
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.math.max
import kotlin.math.min

/**
 * THE TEN NEW MARK TYPES AND THEIR SUB-KINDS (port 62 slice L5, 12 Nov 2026; the approved prototype v61c.2,
 * index61c2.html, module T53: NEW 2397-2407, SUBS 2423-2466, displayName 2478, the code grammar 2534-2543, defaults53
 * 2551-2565, measures53 2569-2588, placeRefusal 2590). Amit, 16 Oct 2026, 06:37 IST: "Need more mark types for plumbing
 * and sanitary ware etc." / "Carpentry mark type is also expected."; 11:45 IST: "Use relevant abbreviation rather than
 * letter." -- so the ten are abbreviations, NOTE GRL AC CDP PLB CTR CRP LV SAN DEF, beside v52's twelve letters
 * W D C B O N L E P S F X; a code is <surface letter><type token><n> (FPLB1, CNOTE2, CCDP1) and every reader takes the
 * WHOLE letter run after the surface letter as the type, never its first letter.
 *
 * Pure data and pure functions, ported whole and pinned ROW BY ROW against the page's own code: MarkTypesTest prints
 * these tables in the form docs/port62/proto-tables.mjs prints T53's, and compares the two line for line.
 * [SurveyPrep.TYPES] appends [typesOn] to v52's rows, [SurveyPrep.isPoint] grows by [isPoint], and
 * [SurveyPrep.defaults] and [SurveyPrep.measures0] ask [defaults] and [measures] here FIRST, exactly as sp.js asks
 * T53 (index61c2.html 4972, 4977). NOTHING ON SCREEN IS BUILT FOR THEM YET: More ▸ by trade, the kind picker on the
 * tool chip, KIND in the Mark sheet, the ten icons and the Note pin are slice L6's, and L5 reaches `main` only together
 * with L6 (DESIGN-port62 §3.5) -- until then the survey prep screens would list the ten as tiles of v52's More grid.
 *
 * WHAT IS STORED, and never translated: a type's TOKEN (a mark's `t`), a sub-kind's ID (a mark's `sub`, written ONLY
 * when it is not the type's first sub-kind on that kind of surface, so a mark nobody gave a kind saves as v52 saved
 * it) and a reading's key (FPLB1.cl). A type's NAME and SHORT name and a sub-kind's LABEL are identity values, the
 * English everything compares, shown through [Masters.type], [Masters.type_short] and [Masters.type_sub].
 *
 * A type's TRADE is [Trades]' (slice L1): [New.trade] is the table's own column, which TradesTest and MarkTypesTest
 * hold equal to [Trades.FIXED]; a Note and a Defect have none of their own (a Note takes its stored cat, a Defect its
 * sub-kind's: [Sub.trade]). Not ported here: the tool's remembered sub-kind (L6), serviceOf (L9), the Trade chip's
 * MarkFilter and paletteMain by trade (DESIGN-port62 Q-list 3).
 */
object MarkTypes {

    const val WALL = "wall"
    const val FLOOR = "floor"
    const val CEIL = "ceil"

    /** The three kinds of surface, in the prototype's order (KINDS). */
    val KINDS: List<String> = listOf(WALL, FLOOR, CEIL)

    /** A surface id (front, right, back, left, floor, ceil) or a kind, as its kind ([Trades.kindOf]). */
    fun kindOf(s: String): String = Trades.kindOf(s)

    /** What a type is on a face: a rectangle with resize tabs; a 50 mm POINT, measured to its centre; or a PIN. */
    enum class Shape { RECT, POINT, PIN }

    /**
     * One of the ten: its token [t]; its [name] and [short] name (identities, shown through [Masters.type] and
     * [Masters.type_short]; the short one fits a palette slot); its [colour] (0xRRGGBB); the [kinds] of surface it may
     * go on; its [trade] (null: its own cat or its sub-kind's, [Trades.tradeOf]); its [shape]; [ink]: its pale colour
     * is outlined in ink on a drawing (Sanitary ware; read in slice L8).
     */
    class New(val t: String, val name: String, val colour: Int, val kinds: List<String>, val short: String,
              val trade: String?, val shape: Shape = Shape.RECT, val ink: Boolean = false)

    /** The Note's token: a pin of words, with no measures and no state ([Trades.NOTE], [MarkLife.NOTE_TYPE]). */
    const val NOTE = Trades.NOTE

    /** The ten, in the prototype's order (NEW): the order they follow v52's rows in on every surface, and in More ▸. */
    val NEW: List<New> = listOf(
        New(NOTE, "Note", 0xffffff, listOf(WALL, FLOOR, CEIL), "Note", null, Shape.PIN), // text:value identity, shown through Masters.type / type_short
        New("GRL", "Grill", 0x82c91e, listOf(WALL), "Grill", Trades.GRILLS), // text:value identity, shown through Masters.type / type_short
        New("AC", "AC unit", 0x99e9f2, listOf(WALL, CEIL), "AC", Trades.HVAC), // text:value identity, shown through Masters.type / type_short
        New("CDP", "Ceiling drop", 0x96f2d7, listOf(CEIL), "Drop", Trades.FALSE_CEILING), // text:value identity, shown through Masters.type / type_short
        New("PLB", "Plumbing point", 0x15aabf, listOf(WALL, FLOOR), "Plumbing", Trades.PLUMBING, Shape.POINT), // text:value identity, shown through Masters.type / type_short
        New("CTR", "Curtain track", 0xc2255c, listOf(WALL, CEIL), "Track", Trades.CURTAINS), // text:value identity, shown through Masters.type / type_short
        New("CRP", "Carpentry unit", 0xa0522d, listOf(WALL, FLOOR), "Unit", Trades.CARPENTRY), // text:value identity, shown through Masters.type / type_short
        New("LV", "Low-voltage point", 0x7048e8, listOf(WALL, CEIL), "LV point", Trades.LOWVOLT, Shape.POINT), // text:value identity, shown through Masters.type / type_short
        New("SAN", "Sanitary ware", 0xd0ebff, listOf(WALL, FLOOR), "Sanitary", Trades.PLUMBING, ink = true), // text:value identity, shown through Masters.type / type_short
        New("DEF", "Defect", 0xf03e3e, listOf(WALL, FLOOR, CEIL), "Defect", null), // text:value identity, shown through Masters.type / type_short
    )

    private val BY_TOKEN: Map<String, New> = NEW.associateBy { it.t }

    /** The ten tokens, in order (TOKENS). */
    val TOKENS: List<String> = NEW.map { it.t }

    /** The one of the ten [t] names, or null: a v52 letter, or a token this build does not know. */
    fun byToken(t: String): New? = BY_TOKEN[t]

    /** Whether [t] is one of the ten (isNew). */
    fun isNew(t: String): Boolean = t in BY_TOKEN

    /** The new POINTS: a Plumbing point and a Low-voltage point, 50 mm spots like E, P and F (isPoint53). */
    fun isPoint(t: String): Boolean = BY_TOKEN[t]?.shape == Shape.POINT

    /** A PIN: the Note -- no measures, no resize tabs, and not a point (isPin). */
    fun isPin(t: String): Boolean = BY_TOKEN[t]?.shape == Shape.PIN

    /** Whether the new type [t] may go on surface (or kind) [s]; null for a type that is not one of the ten, which
     *  [SurveyPrep.TYPES] decides (canGo). */
    fun canGo(t: String, s: String): Boolean? = BY_TOKEN[t]?.let { kindOf(s) in it.kinds }

    /** The new types of one kind of surface as [SurveyPrep.TYPES] rows, in [NEW]'s order (TYPES53): 9 on a wall, 5 on
     *  the floor, 6 on the ceiling -- with v52's 12, 6 and 7: 21, 11 and 13. */
    fun typesOn(kind: String): List<SurveyPrep.MarkType> =
        NEW.filter { kind in it.kinds }.map { SurveyPrep.MarkType(it.t, it.name, it.colour) }

    /** A new type's short name by its name (SHORT53): what [SurveyPrepPalette.SHORT] gains. */
    val SHORT: Map<String, String> = NEW.associate { it.name to it.short }

    /** A type's name in English, the identity: one of the ten's; a v52 letter's AS A WALL HAS IT (every one of the
     *  twelve is a wall's type, so [SurveyPrep.TYPES] is the one table of them); else the token itself. */
    fun nameOf(t: String): String = BY_TOKEN[t]?.name ?: SurveyPrep.TYPES.getValue(WALL).firstOrNull { it.letter == t }?.name ?: t

    /** A type's name in the language, whatever the surface (typeName): S is the Switch plate here -- [displayName]
     *  knows the floor's S is a Step. A token this build does not know is shown as written. */
    fun typeName(tx: Tx, t: String): String = Masters.type(tx, nameOf(t))

    // ------------------------------------------------------------------ sub-kinds

    /** A box as a sub-kind's table gives it: its left and bottom edge, and its size, mm. */
    class Box(val x0: Double, val y0: Double, val w: Double, val h: Double)

    /**
     * ONE SUB-KIND of a type on one kind of surface: its stored [id]; its [label] (an identity, shown through
     * [Masters.type_sub]); and where its default box goes, in the column its type reads ([defaults]) -- [box] a
     * function of the face (a Switch plate's, a Grill's, an AC unit's, a Ceiling drop's); [w] x [h] at height [y] (a
     * Carpentry unit's and a Sanitary ware's; [centre]: in the middle of a floor, not against its edge); a point's
     * centre height [cy], or [fromTop] under the ceiling. [service]: the wish-list service a Low-voltage point of
     * this kind answers ([Trades.SERVICE_TRADE]); [trade]: the trade of a Defect of this kind when it is not
     * Painting's ([Trades.tradeOf]).
     */
    class Sub(val id: String, val label: String, val w: Double? = null, val h: Double? = null, val y: Double? = null,
              val cy: Double? = null, val fromTop: Double? = null, val centre: Boolean = false,
              val service: String? = null, val trade: String? = null, val box: ((a: Double, b: Double) -> Box)? = null)

    private fun mid(a: Double, w: Double) = (a - w) / 2

    private val CURTAINS: List<Sub> = listOf(
        Sub("track", "Curtain track"), Sub("rod", "Rod"), // text:value identity, shown through Masters.type_sub
        Sub("roller", "Roller blind"), Sub("vertical", "Vertical blind")) // text:value identity, shown through Masters.type_sub

    private val DEFECTS: List<Sub> = listOf(
        Sub("crack", "Crack"), Sub("damp", "Damp / seepage"), Sub("hollow", "Hollow plaster"), // text:value identity, shown through Masters.type_sub
        Sub("peeling", "Peeling paint"), Sub(Trades.SUB_TILE, "Broken / hollow tile", trade = Trades.TILING)) // text:value identity, shown through Masters.type_sub

    /**
     * THE SUB-KINDS (SUBS), by type and kind of surface; THE FIRST IS THE DEFAULT, and the one a mark with no `sub`
     * (or one this build does not know) reads as. S has them on a wall only -- the floor's S is a Step. On a wall x
     * runs from the left corner and y up from the floor; on the floor and ceiling y = 0 is the BACK wall.
     */
    val SUBS: Map<String, Map<String, List<Sub>>> = linkedMapOf(
        "S" to mapOf(WALL to listOf(
            Sub("switch", "Switch plate", box = { a, b -> Box(min(150.0, max(0.0, a - 200)), min(1100.0, max(0.0, b - 90)), 200.0, 90.0) }), // text:value identity, shown through Masters.type_sub
            Sub("db", "DB / MCB board", box = { _, _ -> Box(150.0, 1500.0, 300.0, 400.0) }), // text:value identity, shown through Masters.type_sub
            Sub("meter", "Meter board", box = { _, _ -> Box(150.0, 1500.0, 450.0, 600.0) }))), // text:value identity, shown through Masters.type_sub
        "GRL" to mapOf(WALL to listOf(
            Sub("window", "Window grill", box = { a, _ -> Box(mid(a, 1200.0), 900.0, 1200.0, 1200.0) }), // text:value identity, shown through Masters.type_sub
            Sub("balcony", "Balcony grill", box = { a, _ -> min(a, 3000.0).let { w -> Box(mid(a, w), 0.0, w, 1000.0) } }), // text:value identity, shown through Masters.type_sub
            Sub("railing", "Railing", box = { a, _ -> min(a, 3000.0).let { w -> Box(mid(a, w), 0.0, w, 900.0) } }), // text:value identity, shown through Masters.type_sub
            Sub("door", "Safety door grill", box = { _, _ -> Box(300.0, 0.0, 1000.0, 2100.0) }))), // text:value identity, shown through Masters.type_sub
        "AC" to mapOf(
            WALL to listOf(
                Sub("split", "Split indoor unit", box = { a, b -> Box(mid(a, 1000.0), b - 150 - 300, 1000.0, 300.0) }), // text:value identity, shown through Masters.type_sub
                Sub("window-ac", "Window AC", box = { a, _ -> Box(mid(a, 600.0), 1200.0, 600.0, 450.0) }), // text:value identity, shown through Masters.type_sub
                Sub("outdoor", "Outdoor unit", box = { a, _ -> Box(mid(a, 800.0), 0.0, 800.0, 600.0) })), // text:value identity, shown through Masters.type_sub
            CEIL to listOf(
                Sub("cassette", "Cassette", box = { a, b -> Box(mid(a, 600.0), mid(b, 600.0), 600.0, 600.0) }), // text:value identity, shown through Masters.type_sub
                Sub("duct", "Ductable grille", box = { a, b -> Box(mid(a, 1000.0), mid(b, 200.0), 1000.0, 200.0) }))), // text:value identity, shown through Masters.type_sub
        "CDP" to mapOf(CEIL to listOf(
            Sub("bulkhead", "Bulkhead", box = { a, b -> Box(0.0, b - 600, a, 600.0) }), // text:value identity, shown through Masters.type_sub
            Sub("cove", "Cove", box = { a, b -> Box(0.0, b - 300, a, 300.0) }), // text:value identity, shown through Masters.type_sub
            Sub("full", "Full false ceiling", box = { a, b -> Box(300.0, 300.0, a - 600, b - 600) }))), // text:value identity, shown through Masters.type_sub
        "PLB" to mapOf(
            WALL to listOf(
                Sub("cold", "Cold", cy = 900.0), Sub("hot", "Hot", cy = 900.0), Sub("mixer", "Mixer (hot + cold)", cy = 1000.0), // text:value identity, shown through Masters.type_sub
                Sub("wm", "Washing machine", cy = 1000.0), Sub("ro", "RO / purifier", cy = 1200.0), Sub("waste", "Waste outlet", cy = 400.0)), // text:value identity, shown through Masters.type_sub
            FLOOR to listOf(
                Sub("trap", "Floor trap"), Sub("waste", "Waste outlet"), Sub("cold", "Cold"), Sub("hot", "Hot"))), // text:value identity, shown through Masters.type_sub
        "CTR" to mapOf(WALL to CURTAINS, CEIL to CURTAINS),
        "CRP" to mapOf(
            WALL to listOf(
                Sub("wardrobe", "Wardrobe", w = 1200.0, h = 2100.0, y = 0.0), Sub("kbase", "Kitchen base", w = 1800.0, h = 850.0, y = 0.0), // text:value identity, shown through Masters.type_sub
                Sub("kwall", "Kitchen wall unit", w = 1800.0, h = 700.0, y = 1500.0), Sub("tv", "TV unit", w = 1800.0, h = 450.0, y = 400.0), // text:value identity, shown through Masters.type_sub
                Sub("study", "Study", w = 1200.0, h = 750.0, y = 0.0), Sub("shoe", "Shoe rack", w = 900.0, h = 1000.0, y = 0.0), // text:value identity, shown through Masters.type_sub
                Sub("vanity", "Vanity", w = 900.0, h = 500.0, y = 400.0), Sub("other", "Other unit", w = 600.0, h = 600.0, y = 0.0)), // text:value identity, shown through Masters.type_sub
            FLOOR to listOf(
                Sub("bed", "Bed", w = 1800.0, h = 2000.0), Sub("platform", "Kitchen platform", w = 2400.0, h = 600.0), // text:value identity, shown through Masters.type_sub
                Sub("island", "Island", w = 1200.0, h = 800.0, centre = true), Sub("other", "Other unit", w = 1200.0, h = 600.0))), // text:value identity, shown through Masters.type_sub
        "LV" to mapOf(
            WALL to listOf(
                Sub("camera", "Camera", service = "cctv", fromTop = 325.0), // text:value identity, shown through Masters.type_sub
                Sub("intercom", "Intercom / video door phone", service = "intercom", cy = 1400.0), // text:value identity, shown through Masters.type_sub
                Sub("data", "Data / LAN", service = "cctv", cy = 300.0), Sub("tv", "TV point", service = "cctv", cy = 1200.0), // text:value identity, shown through Masters.type_sub
                Sub("wifi", "Wi-Fi access point", service = "cctv", fromTop = 325.0)), // text:value identity, shown through Masters.type_sub
            CEIL to listOf(
                Sub("camera", "Camera", service = "cctv"), Sub("wifi", "Wi-Fi access point", service = "cctv"), // text:value identity, shown through Masters.type_sub
                Sub("smoke", "Smoke detector", service = "cctv"))), // text:value identity, shown through Masters.type_sub
        "SAN" to mapOf(
            WALL to listOf(
                Sub("basin", "Wash basin", w = 550.0, h = 450.0, y = 400.0), Sub("wc-wall", "Wall-hung WC", w = 360.0, h = 400.0, y = 300.0), // text:value identity, shown through Masters.type_sub
                Sub("shower", "Shower", w = 300.0, h = 300.0, y = 1950.0), Sub("geyser", "Geyser", w = 450.0, h = 600.0, y = 1800.0), // text:value identity, shown through Masters.type_sub
                Sub("sink", "Kitchen sink", w = 800.0, h = 200.0, y = 850.0), Sub("mirror", "Mirror cabinet", w = 600.0, h = 750.0, y = 1200.0)), // text:value identity, shown through Masters.type_sub
            FLOOR to listOf(
                Sub("wc", "WC", w = 400.0, h = 700.0, centre = true), Sub("shower-area", "Shower area", w = 900.0, h = 900.0), // text:value identity, shown through Masters.type_sub
                Sub("tub", "Bathtub", w = 1700.0, h = 750.0))), // text:value identity, shown through Masters.type_sub
        "DEF" to KINDS.associateWith { DEFECTS },
    )

    /** The sub-kinds of type [t] on surface (or kind) [s], the default first; none for a type that has none there (subsOf). */
    fun subsOf(t: String, s: String): List<Sub> = SUBS[t]?.get(kindOf(s)) ?: emptyList()

    /** The sub-kind [sub] of [t] on [s]; AN UNKNOWN OR ABSENT ONE READS AS THE FIRST; null where the type has none (subInfo). */
    fun subInfo(t: String, s: String, sub: String?): Sub? = subsOf(t, s).let { l -> l.firstOrNull { it.id == sub } ?: l.firstOrNull() }

    /** The id of the first sub-kind of [t] on [s], its default; null where it has none (firstSub). */
    fun firstSub(t: String, s: String): String? = subsOf(t, s).firstOrNull()?.id

    /**
     * What a mark STORES for the sub-kind [sub] (subToStore): the id only when it is one of the type's on that kind of
     * surface and NOT the first -- so a mark of the default kind carries no `sub` at all and saves as v52 saved it.
     */
    fun subToStore(t: String, s: String, sub: String?): String? =
        sub?.takeIf { it.isNotEmpty() && it != firstSub(t, s) && subsOf(t, s).any { x -> x.id == it } }

    /** The sub-kind the mark [e] on [s] IS: its stored `sub`, or the first when it stores none or one this build does
     *  not know; null for a type with no sub-kinds there. */
    fun subOf(s: String, e: SurveyPrep.Mark): Sub? = subInfo(e.t, s, Trades.subOf(e.extra))

    /** [extra] with the sub-kind [sub] of type [t] on [s] as it is stored ([subToStore]): the first, an unknown one or
     *  none DELETES the key. A NEW map: the old one may be shared by an undo copy. */
    fun withSub(extra: Map<String, Any?>, t: String, s: String, sub: String?): Map<String, Any?> =
        LinkedHashMap(extra).apply { subToStore(t, s, sub)?.let { put(Trades.KEY_SUB, it) } ?: remove(Trades.KEY_SUB) }

    /** Gives the mark [e] on [s] the sub-kind [sub] ([withSub]). The box is the caller's: a kind changed in the Mark
     *  sheet keeps the box the mark has (slice L6). */
    fun setSub(s: String, e: SurveyPrep.Mark, sub: String?) { e.extra = withSub(e.extra, e.t, s, sub) }

    /** A sub-kind's label in the language. */
    fun subLabel(tx: Tx, sub: Sub): String = Masters.type_sub(tx, sub.label)

    /**
     * THE NAME A MARK SHOWS (displayName): its type's -- or its sub-kind's label where the type has sub-kinds on that
     * kind of surface ("Wardrobe", "Full false ceiling"; an unknown or absent `sub` reads as the first). A Plumbing
     * point keeps its type's name first: "Plumbing point · Cold". The floor's S is a Step.
     */
    fun displayName(tx: Tx, t: String, s: String, sub: String?): String {
        val k = kindOf(s)
        if (t == "S" && k == FLOOR) return tx.type_s_floor
        val si = subInfo(t, k, sub) ?: return typeName(tx, t)
        val label = subLabel(tx, si)
        return if (t == "PLB") tx.t53_plumbing_point_x(label) else label
    }

    /** [displayName] of the mark [e] on [s]. */
    fun displayName(tx: Tx, s: String, e: SurveyPrep.Mark): String = displayName(tx, e.t, s, Trades.subOf(e.extra))

    // ------------------------------------------------------------------ the code grammar

    /** A surface's code letter -> the surface (LETTER_SURF; [SurveyPrep.CODE] the other way). */
    private val LETTER_SURF: Map<String, String> = mapOf("F" to "front", "R" to "right", "B" to "back", "L" to "left", "G" to "floor", "C" to "ceil")

    /** What a parsed code names. */
    const val KIND_MARK = "mark"
    const val KIND_LINE = "line"
    const val KIND_TAPE = "tape"
    const val KIND_CHAIN = "chain"

    /**
     * A code, or a reading key, taken apart: its [surface] letter and the surface [surf] it names; its type [token] --
     * the WHOLE letter run after the surface letter (null for a Line's or a Tape's code); what it is ([kind]:
     * [KIND_MARK], [KIND_CHAIN] for the letter K, [KIND_LINE] for F-M1, [KIND_TAPE] for F-T1); its number [n]; and the
     * [measure] after the dot (null for a bare code).
     */
    class Code(val surface: String, val surf: String, val token: String?, val kind: String, val n: Int, val measure: String?)

    private val LINE_RE = Regex("^([FRBLGC])-([MT])(\\d+)(?:\\.([A-Za-z0-9]+))?$") // text:value a code's grammar, never shown
    private val CODE_RE = Regex("^([FRBLGC])([A-Za-z]+)(\\d+)(?:\\.([A-Za-z0-9]+))?$") // text:value a code's grammar, never shown

    /**
     * THE CODE GRAMMAR (parseCode): <surface letter><type token><n>, with .<measure> after it for a reading key --
     * FW1, FPLB12.cl, CCDP1, LLV3 (a Low-voltage point on the LEFT wall; LL1 is its Loft). Lines (F-M1), Tapes (F-T1)
     * and Chains (FK1) are not marks and say so with [Code.kind]. Null for anything else.
     */
    fun parseCode(code: String?): Code? {
        val s = code ?: return null
        LINE_RE.matchEntire(s)?.let { m ->
            val g = m.groupValues
            return Code(g[1], LETTER_SURF.getValue(g[1]), null, if (g[2] == "M") KIND_LINE else KIND_TAPE, g[3].toIntOrNull() ?: return null, g[4].ifEmpty { null })
        }
        val g = CODE_RE.matchEntire(s)?.groupValues ?: return null
        return Code(g[1], LETTER_SURF.getValue(g[1]), g[2], if (g[2] == SurveyChain.LETTER) KIND_CHAIN else KIND_MARK, g[3].toIntOrNull() ?: return null, g[4].ifEmpty { null })
    }

    /** The type token of a code: the whole letter run after its surface letter (FPLB1 -> PLB, never P); null for a
     *  Line's, a Tape's, or what is no code (codeToken). */
    fun codeToken(code: String?): String? = parseCode(code)?.token

    // ------------------------------------------------------------------ default boxes

    /** [bx] no larger than the [a] x [b] face (and never under 1 mm) and slid back inside it: x0, x1, y0, y1 (clampBox). */
    private fun clamp(bx: Box, a: Double, b: Double): DoubleArray {
        val w = max(1.0, min(a, bx.w)); val h = max(1.0, min(b, bx.h))
        val x0 = max(0.0, min(a - w, bx.x0)); val y0 = max(0.0, min(b - h, bx.y0))
        return doubleArrayOf(x0, x0 + w, y0, y0 + h)
    }

    /** A 50 mm spot centred at ([cx], [cy]), kept on the face (spotAt). */
    private fun spotAt(cx: Double, cy: Double, a: Double, b: Double): DoubleArray =
        clamp(Box(cx - SurveyPrep.SPOT / 2, cy - SurveyPrep.SPOT / 2, SurveyPrep.SPOT, SurveyPrep.SPOT), a, b)

    /** The 600 mm square in the middle of the face: where a new type starts on a surface its table does not give. */
    private fun square(a: Double, b: Double): DoubleArray = clamp(Box(mid(a, 600.0), mid(b, 600.0), 600.0, 600.0), a, b)

    /**
     * WHERE A NEW MARK OF A NEW TYPE STARTS (defaults53): the default box of type [t], sub-kind [sub] (absent or
     * unknown: the first), on surface [s] of [a] x [b] mm -- x0, x1, y0, y1, always inside the face. NULL WHEN v52's
     * OWN RULE DECIDES ([SurveyPrep.defaults]): every v52 letter, and S with no sub-kind or `switch`, so a v52 mark
     * starts exactly where it did. S's two new sub-kinds, a DB / MCB board and a Meter board, have their boxes here.
     * A Note is a pin, stored as a 50 mm box in the middle (a tap puts it where the finger is: [SurveyPrep.placeAt]).
     */
    fun defaults(s: String, t: String, sub: String?, a: Double, b: Double): DoubleArray? {
        val k = kindOf(s)
        if (t == "S") {
            if (k != WALL || sub.isNullOrEmpty() || sub == "switch") return null
            return subInfo(t, k, sub)?.box?.let { clamp(it(a, b), a, b) }
        }
        val n = BY_TOKEN[t] ?: return null
        if (k !in n.kinds) return square(a, b)
        val si = subInfo(t, k, sub)
        return when (t) {
            NOTE -> spotAt(a / 2, b / 2, a, b)
            "PLB" -> if (k == WALL) spotAt(min(325.0, a / 2), si?.cy ?: 900.0, a, b) else spotAt(a / 2, b / 2, a, b)
            "LV" -> if (k == WALL) spotAt(a - 325, si?.fromTop?.let { b - it } ?: si?.cy ?: (b - 325), a, b) else spotAt(a / 2, b / 2, a, b)
            "CTR" -> { val w = min(1500.0, a); clamp(Box(mid(a, w), if (k == WALL) b - 150 - 100 else b - 300 - 100, w, 100.0), a, b) }
            "CRP" -> { val w = si!!.w!!; val h = si.h!!
                if (k == WALL) clamp(Box(150.0, si.y!!, w, h), a, b)
                else clamp(Box(if (si.centre) mid(a, w) else 150.0, if (si.centre) mid(b, h) else 0.0, w, h), a, b) }
            "SAN" -> { val w = si!!.w!!; val h = si.h!!
                if (k == WALL) clamp(Box(mid(a, w), si.y!!, w, h), a, b)
                else clamp(Box(if (si.centre) mid(a, w) else 0.0, 0.0, w, h), a, b) }
            "DEF" -> clamp(Box(mid(a, 300.0), mid(b, 300.0), 300.0, 300.0), a, b)
            else -> si?.box?.let { clamp(it(a, b), a, b) } ?: square(a, b)
        }
    }

    // ------------------------------------------------------------------ measures

    /**
     * WHAT A MARK OF A NEW TYPE ASKS THE ENGINEER TO MEASURE (measures53), before what is zero is dropped
     * ([SurveyPrep.measures]): key, what it is, and the photo's estimate (null: site only). NULL FOR A v52 TYPE, whose
     * own checklist [SurveyPrep.measures0] keeps (S with any sub-kind measures as v52's S). A Note has NONE. A
     * Plumbing point and a Low-voltage point are spots, measured to their CENTRE from the nearer side, under the
     * centre keys (cl cr cs ct on a wall, cl cr cf ck on a plan), as E, P and F are. Every other new type gives its
     * width and height (a plan's: left–right and front–back), its nearer side, and what its trade needs -- a Grill
     * and a unit their height from the floor, a Curtain track its length and its drop from the ceiling, and a `p` the
     * photo cannot show: projection, depth, drop below the ceiling or the slab.
     */
    fun measures(s: String, e: SurveyPrep.Mark, a: Double, b: Double): List<SurveyPrep.Measure>? {
        val t = e.t
        if (!isNew(t)) return null
        if (t == NOTE) return emptyList()
        val plan = kindOf(s) != WALL; val pt = isPoint(t)
        val w = e.x1 - e.x0; val h = e.y1 - e.y0
        val cx = (e.x0 + e.x1) / 2; val cy = (e.y0 + e.y1) / 2
        // ", to centre" after a point's two offsets, joined as SurveyPrep.measures0 joins measure.to_centre
        val to: Msg = if (pt) { tx -> tx.measure_to_centre } else { _ -> "" }
        val l = if (pt) cx else e.x0; val r = if (pt) a - cx else a - e.x1
        val bo = if (pt) cy else e.y0; val tp = if (pt) b - cy else b - e.y1
        val hx = if (l <= r) SurveyPrep.Measure("l", { tx -> (if (plan) tx.measure_left_wall else tx.measure_left_corner) + to(tx) }, l)
                 else SurveyPrep.Measure("r", { tx -> (if (plan) tx.measure_right_wall else tx.measure_right_corner) + to(tx) }, r)
        val vy = if (plan) (if (tp <= bo) SurveyPrep.Measure("f", { tx -> tx.measure_front_wall + to(tx) }, tp)
                            else SurveyPrep.Measure("k", { tx -> tx.measure_back_wall + to(tx) }, bo))
                 else if (bo <= tp) SurveyPrep.Measure("s", { tx -> tx.measure_from_floor + to(tx) }, bo)
                 else SurveyPrep.Measure("t", { tx -> tx.measure_from_ceiling + to(tx) }, tp)
        if (pt) return listOf(hx, vy).map { SurveyPrep.Measure(SurveyPrep.ckey(plan, t, it.key), it.what, it.est) }
        val width = SurveyPrep.Measure("w", { tx -> if (plan) tx.measure_width_left_right else tx.measure_width }, w)
        val height = SurveyPrep.Measure("h", { tx -> if (plan) tx.measure_depth_front_back else tx.measure_height }, h)
        val track = SurveyPrep.Measure("w", { tx -> tx.measure_track_length }, w)
        fun p(what: Msg) = SurveyPrep.Measure("p", what, null)
        if (!plan) return when (t) {
            "GRL" -> listOf(width, height, hx, SurveyPrep.Measure("s", { tx -> tx.measure_from_floor }, e.y0), p { tx -> tx.measure_projection_wall })
            "AC" -> listOf(width, height, hx, vy, p { tx -> tx.measure_projection_wall })
            "CTR" -> listOf(track, hx, SurveyPrep.Measure("t", { tx -> tx.measure_from_ceiling }, b - e.y1), p { tx -> tx.measure_projection_wall })
            "CRP" -> listOf(width, height, hx, SurveyPrep.Measure("s", { tx -> tx.measure_underside_floor }, e.y0), p { tx -> tx.measure_depth_wall })
            "SAN" -> listOf(width, height, hx, SurveyPrep.Measure("s", { tx -> tx.measure_underside_floor }, e.y0), p { tx -> tx.measure_projection_wall })
            else -> listOf(width, height, hx, vy)   // a Defect (and a Ceiling drop, which no wall takes)
        }
        return when (t) {
            "AC" -> listOf(width, height, hx, vy, p { tx -> tx.measure_drop_below_ceiling })
            "CDP" -> listOf(width, height, hx, vy, p { tx -> tx.measure_drop_below_slab })
            "CTR" -> listOf(track, hx, vy)
            "CRP", "SAN" -> listOf(width, height, hx, vy, p { tx -> tx.measure_height })
            else -> listOf(width, height, hx, vy)   // a Defect (and a Grill, which no plan takes)
        }
    }

    // ------------------------------------------------------------------ where a new type may go

    /**
     * 4 · 360: A NEW TYPE GOES ONLY WHERE IT CAN (placeRefusal). Null when [t] may go on surface (or kind) [to] -- and
     * for a type that is not one of the ten, whose rule is its MEANING ([Pano360.placeRefusal] asks that next); else
     * the words the screen says: "A Ceiling drop goes on the ceiling only" for a type that goes on the ceiling alone,
     * "An AC unit cannot go on the floor" for the rest.
     *
     * The article is English's: the prototype picks `A` / `An` in code, so each is its own key (t53.a, t53.an), and
     * Marathi has none -- the two templates' own notes say "{a} is the English article — supply ''" -- which leaves the
     * Marathi sentence starting at the name ("AC युनिट जमिनीवर लावता येत नाही").
     */
    fun placeRefusal(tx: Tx, t: String, to: String): String? {
        val k = kindOf(to); val n = BY_TOKEN[t] ?: return null
        if (k in n.kinds) return null
        val a = if (n.name.first() in "AEIOU") tx.t53_an else tx.t53_a
        val name = Masters.type(tx, n.name)
        val said = if (n.kinds.size == 1 && n.kinds[0] == CEIL) tx.refuse_t53_x_x_goes_ceiling(a = a, name = name)
                   else tx.refuse_t53_x_x_cannot_go(a = a, name = name,
                       v = when (k) { CEIL -> tx.t53_the_ceiling; FLOOR -> tx.t53_the_floor; else -> tx.t53_a_wall })
        return said.trimStart()
    }
}
