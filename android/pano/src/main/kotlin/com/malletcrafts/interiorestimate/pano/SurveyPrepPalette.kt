package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Masters
import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * THE 2 · MARK PALETTE (prototype v52, sp.js 1356-1399; v52 slice 7). One row at the bottom, within thumb
 * reach, exactly one tool on; the first is Select and it is the default. A type tool is STICKY: every tap on
 * empty photo places one more, the new one selected, while a touch on an existing mark still picks and moves
 * it. Tapping another tool switches at once; tapping the one already on goes back to Select (as ✕ on its
 * chip does). The types that do not fit are in More ▸, a sheet of every type of the surface with its name.
 *
 * Amit, 14 Oct 2026 11:48 IST (item 5): "i am getting confused between measure. add one more tool called as
 * Tape which act as a actual tape and can plot measurements like a real tape measure"; 20:45: "i had requested
 * two separate tools. one tape measure like anothr is actual marking simple line." -- so v52's palette is
 * Select, Tape, Line, Chain, THREE types and More ▸: eight slots, every name whole at 360 dp. On walls the
 * types are Window, Door and Electric (Loft moved into More); on the floor Column, Step and Electric (Bulb into
 * More); on the ceiling Beam, Electric and Bulb (Column into More).
 *
 * A SLOT APPEARS ONLY WHEN ITS TOOL WORKS (no dead buttons): Tape and Line joined in slice 9 and Chain in slice 11
 * (v52 slice 11: the palette is COMPLETE -- Select, Tape, Line, Chain, three types, More ▸: exactly eight slots).
 *
 * Pure, so the slots, the More order, the labels and the sticky rule are tested here; SpPalette only draws.
 */
object SurveyPrepPalette {

    /** The Select tool's key; a type tool's key is its letter. */
    const val SEL = "sel"

    /** The drawing tools, in palette order: Tape "T" and Line "M" (v52 slice 9), Chain "K" (v52 slice 11). */
    const val TAPE = "T"
    const val LINE = "M"
    const val CHAIN = "K"
    val TOOLS: List<String> = listOf(TAPE, LINE, CHAIN)

    /** Tape or Line -- the two that pull a figure by a drag (SurveyMeas); the Chain is tapped point by point. */
    fun isMeasTool(tool: String) = tool == TAPE || tool == LINE

    /** A drawing tool's name on its slot (sp.js 1381): never "Measure" -- the v46 Measure tool is the Line (item 6). */
    fun toolName(tx: Tx, key: String) = when (key) { TAPE -> tx.common_tape; LINE -> tx.common_line; CHAIN -> tx.common_chain; else -> key }

    /** A drawing tool's colour: tape yellow, Line blue, chain rose (sp.js TC, MC, KC). */
    fun toolColour(key: String) = when (key) {
        TAPE -> SurveyMeas.TAPE_COLOUR; LINE -> SurveyMeas.LINE_COLOUR; CHAIN -> SurveyChain.COLOUR; else -> 0xcfd3cb }

    /** Whether [tool] is a mark type (a tap places one), not Select nor a drawing tool. */
    fun isType(tool: String) = tool != SEL && tool !in TOOLS

    /** The colour the tool chip is ringed in with [tool] on, on [s]. */
    fun chipColour(s: String, tool: String) = if (tool in TOOLS) toolColour(tool) else SurveyPrep.typeOf(s, tool).colour

    /** A type's short name, by its name (sp.js SHORT): the identity its slot and its More tile show through
     *  [Masters.type_short] (port 61c slice M3a). */
    val SHORT = mapOf("Window" to "Window", "Door" to "Door", "Column" to "Column", "Beam" to "Beam", // text:value identity, shown through Masters.type_short
        "Opening" to "Opening", "Niche" to "Niche", "Loft" to "Loft", "Electric point" to "Electric", // text:value identity, shown through Masters.type_short
        "Bulb point" to "Bulb", "Other" to "Other", "Step" to "Step", "Switch plate" to "Switch", "Fan" to "Fan") // text:value identity, shown through Masters.type_short

    /** A type's short name as its slot and its More tile print it, in the language. */
    fun short(tx: Tx, name: String) = Masters.type_short(tx, SHORT[name] ?: name)

    /** The three types on the palette itself, per kind of surface (sp.js MAINT). */
    val MAIN = mapOf("wall" to listOf("W", "D", "E"), "floor" to listOf("C", "S", "E"), "ceil" to listOf("B", "E", "P"))

    /** [main]: the three slots, in [MAIN] order; [more]: the rest, in TYPES order; [all]: every type, which the
     *  More sheet lists ("Every mark type · Front wall"). */
    class Palette(val main: List<SurveyPrep.MarkType>, val more: List<SurveyPrep.MarkType>, val all: List<SurveyPrep.MarkType>)

    fun paletteFor(kind: String): Palette {
        val all = SurveyPrep.TYPES.getValue(kind); val m = MAIN.getValue(kind)
        return Palette(m.map { t -> all.first { it.letter == t } }, all.filter { it.letter !in m }, all)
    }

    fun forSurface(s: String) = paletteFor(SurveyPrep.kind(s))

    /** What a slot is: Select, a drawing tool, a type, or More ▸. */
    enum class SlotKind { SELECT, TOOL, TYPE, MORE }

    /** One slot: what it is, the tool it turns on ([SEL], a tool's key, a type's letter; "" for More), its type. */
    class Slot(val kind: SlotKind, val key: String, val type: SurveyPrep.MarkType? = null)

    /** The palette's slots, left to right: Select, the working [tools], the three types, More ▸. */
    fun slots(kind: String, tools: List<String> = TOOLS): List<Slot> {
        val p = paletteFor(kind)
        return listOf(Slot(SlotKind.SELECT, SEL)) + tools.map { Slot(SlotKind.TOOL, it) } +
            p.main.map { Slot(SlotKind.TYPE, it.letter, it) } + (if (p.more.isEmpty()) emptyList() else listOf(Slot(SlotKind.MORE, "")))
    }

    /** The tool after a tap on [picked] with [current] on: sticky, and the tool already on goes back to Select. */
    fun next(current: String, picked: String): String = if (picked != SEL && picked == current) SEL else picked

    /** The More type now on, or null: the More slot is then lit in that type's colour and names it. */
    fun moreArmed(kind: String, tool: String): SurveyPrep.MarkType? =
        if (tool == SEL || tool in TOOLS) null else paletteFor(kind).more.firstOrNull { it.letter == tool }

    /** The More slot's name: "More ▸" (common.more), or the armed More type's short name and ▾ after a NARROW NO-BREAK
     *  SPACE (U+202F), so "Opening ▾" fits its slot (sp.js 1453, ct52 FX8). */
    fun moreLabel(tx: Tx, kind: String, tool: String): String = moreArmed(kind, tool)?.let { short(tx, it.name) + " ▾" } ?: tx.common_more

    /** Is [slot] lit with [tool] on? */
    fun isOn(slot: Slot, kind: String, tool: String): Boolean = when (slot.kind) {
        SlotKind.SELECT -> tool == SEL
        SlotKind.TOOL, SlotKind.TYPE -> slot.key == tool
        SlotKind.MORE -> moreArmed(kind, tool) != null
    }

    /** What the tool chip over the photo says with a type on (sp.js 1454); null with Select, which shows no chip.
     *  [where]: "photo" on 2 · Mark, "360" on 4 · 360 (v52 slice 17, ct52 G: "Placing Window · tap the 360"). */
    fun chipText(tx: Tx, s: String, tool: String, where: String = "photo"): String? =
        if (tool == SEL || tool in TOOLS) null
        // "photo" or "360" is an English word chosen in code, so each is its own whole key (the card's group 2)
        else if (where == "360") tx.sp_placing_x_tap_360(tool = SurveyPrep.typeName(tx, s, tool))
        else tx.sp_placing_x_tap_photo(tool = SurveyPrep.typeName(tx, s, tool))

    /** The More sheet's heading: "Every mark type · Front wall". */
    fun sheetTitle(tx: Tx, s: String) = tx.sp_every_mark_type_x(T = Masters.surface(tx, SurveyPrep.NAME.getValue(s)))
}
