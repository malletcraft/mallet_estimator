package com.malletcrafts.sitephotos.pano

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
 * A SLOT APPEARS ONLY WHEN ITS TOOL WORKS (no dead buttons): this build (slice 9) has Select, Tape, Line, the
 * three types and More ▸ -- seven slots; Chain arrives in slice 11 by joining [TOOLS] -- [slots] already places
 * it between Line and the types, so the eighth slot needs no other change.
 *
 * Pure, so the slots, the More order, the labels and the sticky rule are tested here; SpPalette only draws.
 */
object SurveyPrepPalette {

    /** The Select tool's key; a type tool's key is its letter. */
    const val SEL = "sel"

    /** The drawing tools that work in this build, in palette order: Tape "T" and Line "M" (v52 slice 9); Chain "K"
     *  joins in slice 11. */
    const val TAPE = "T"
    const val LINE = "M"
    val TOOLS: List<String> = listOf(TAPE, LINE)

    /** A drawing tool's name on its slot (sp.js 1381): never "Measure" -- the v46 Measure tool is the Line (item 6). */
    fun toolName(key: String) = when (key) { TAPE -> "Tape"; LINE -> "Line"; else -> key }

    /** A drawing tool's colour: tape yellow, Line blue (sp.js TC, MC). */
    fun toolColour(key: String) = when (key) { TAPE -> SurveyMeas.TAPE_COLOUR; LINE -> SurveyMeas.LINE_COLOUR; else -> 0xcfd3cb }

    /** Whether [tool] is a mark type (a tap places one), not Select nor a drawing tool. */
    fun isType(tool: String) = tool != SEL && tool !in TOOLS

    /** The colour the tool chip is ringed in with [tool] on, on [s]. */
    fun chipColour(s: String, tool: String) = if (tool in TOOLS) toolColour(tool) else SurveyPrep.typeOf(s, tool).colour

    /** A type's name as its slot and its More tile print it (sp.js SHORT). */
    val SHORT = mapOf("Window" to "Window", "Door" to "Door", "Column" to "Column", "Beam" to "Beam",
        "Opening" to "Opening", "Niche" to "Niche", "Loft" to "Loft", "Electric point" to "Electric",
        "Bulb point" to "Bulb", "Other" to "Other", "Step" to "Step", "Switch plate" to "Switch", "Fan" to "Fan")

    fun short(name: String) = SHORT[name] ?: name

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

    /** The More slot's name: "More ▸", or the armed More type's short name and ▾ after a NARROW NO-BREAK SPACE
     *  (U+202F), so "Opening ▾" fits its slot (sp.js 1453, ct52 FX8). */
    fun moreLabel(kind: String, tool: String): String = moreArmed(kind, tool)?.let { short(it.name) + " ▾" } ?: "More ▸"

    /** Is [slot] lit with [tool] on? */
    fun isOn(slot: Slot, kind: String, tool: String): Boolean = when (slot.kind) {
        SlotKind.SELECT -> tool == SEL
        SlotKind.TOOL, SlotKind.TYPE -> slot.key == tool
        SlotKind.MORE -> moreArmed(kind, tool) != null
    }

    /** What the tool chip over the photo says with a type on (sp.js 1454); null with Select, which shows no chip. */
    fun chipText(s: String, tool: String): String? =
        if (tool == SEL || tool in TOOLS) null else "Placing ${SurveyPrep.typeOf(s, tool).name} · tap the photo"

    /** The More sheet's heading. */
    fun sheetTitle(s: String) = "Every mark type · ${SurveyPrep.NAME.getValue(s)}"
}
