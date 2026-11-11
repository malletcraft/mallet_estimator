package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * THE GUIDED TOURS' CONTRACT AND CONTENT (port 61c §31) — the prototype's `tour55-flows.js`
 * (index61c2.html, "THE SEVEN FLOWS") as data, word for word, through the Tx accessors the
 * tables already carry (160 `tour.*` keys, landed with L1/L2, Marathi approved).
 *
 * A FLOW is `{id, name, line, steps}`; a STEP is `{id, screen, anchor, title, body, mode,
 * setup, done?}` (no step of the seven carries a `next` move — checked against the page).
 * A MOVE is one key of the prototype's position object; the engine runs them in ITS fixed
 * order whatever the literal order, and the listed order here is the literal's, which for
 * the shipped flow is the same. An ANCHOR is a NAME from one vocabulary ([TourAnchors]),
 * never a selector: the phone tags the same elements with `Modifier.tourAnchor("<name>")`.
 *
 * `ships` says whether every anchor of the flow exists on this build (the port rule: a UI
 * state the approved prototype does not draw does not reach the phone — and its mirror, a
 * step whose target the phone does not draw cannot run true). A blocked flow keeps its data
 * here, `missing` naming what is absent, so adding it later is flipping the entry after its
 * targets land, not re-deriving it. [TourFlowsTest] keeps the shipped set honest.
 */
data class TourMove(val kind: String, val arg: String? = null)

/** A step's auto-advance rule, as the engine's `doneNow` reads it. */
sealed class TourDone {
    /** A tap inside the spotlight after the step showed; the step advances 320 ms AFTER the tap
     *  (a settle delay, so the app's response is seen before the bubble moves — the engine's
     *  `run.tapAt>run.shownAt && now-run.tapAt>=320`, §31 critic finding 4). */
    object Tap : TourDone()
    data class Exists(val anchor: String) : TourDone()
    data class Gone(val anchor: String) : TourDone()
    data class Screen(val screen: String) : TourDone()
    data class Phase(val n: Int) : TourDone()
    data class Tool(val tool: String) : TourDone()
    data class Tab(val tab: String) : TourDone()
    data class Mode(val mode: String) : TourDone()
    data class Marks(val plus: Int) : TourDone()
}

data class TourStep(
    val id: String,
    val screen: String,
    val anchor: String?,
    val mode: String,                     // "do" | "look"
    val setup: List<TourMove>,
    val done: TourDone? = null,
    val title: String,
    val body: String,
)

data class TourFlow(
    val id: String,
    val name: String,
    val line: String,
    val steps: List<TourStep>,
    /** True when every step's target exists on this build, so the Help screen lists it. */
    val ships: Boolean,
    /** When blocked: the missing targets, named, so flipping `ships` later starts from a list. */
    val missing: String? = null,
)

/**
 * THE ANCHOR VOCABULARY — the engine's NAMED list, as a grammar. A name with a parameter is
 * `<name>:<parameter>`; resolution takes the LONGEST base that prefixes the name (so
 * `plan:room:Guest Bedroom` is `plan:room` with `Guest Bedroom`). Two anchors are VIRTUAL —
 * `area:<surface>:x0,y0,x1,y1` (fractions of a surface, y from the floor up, lit wherever
 * that surface is drawn now) and `mark:<code>` — computed with survey prep's own drawing
 * maths, not a tag. No shipped flow names a virtual anchor on this build (the `survey` flow
 * alone ships), so the phone computes neither yet.
 */
object TourAnchors {
    /** Bases that stand alone, no parameter. */
    val BARE = setOf(
        "help", "menu", "capture360", "phase:strip", "phase:ask", "phase:band", "phase:packs",
        "details", "stg", "chip:wish", "quote", "wish:head", "plan:tile", "plan:rotate",
        "plan:fit", "plan:reset", "save", "lines:row", "palette", "chips", "reading:mm",
        "reading:d2", "show", "lock", "photo", "pano", "sheet", "sheet:act", "sheet:acts",
        "sheet:done", "trade", "list:state", "list:move", "list:export", "list:pop",
        "x53", "x53:trades", "x53:fmt",
    )
    /** Bases that REQUIRE a parameter. */
    val PARAM = setOf(
        "flow", "start", "row", "room", "cap", "p54seg", "pack", "service", "service:row",
        "plan:room", "tab", "tool", "sheet:move", "x53:pack",
    )
    /** Bases that take one or stand alone (`mode` the strip / `mode:lines` one step; `toolchip:as`). */
    val OPTIONAL = setOf("mode", "toolchip", "more", "selrow")

    private val AREA = Regex("^area:[a-z]+:-?[0-9.]+,-?[0-9.]+,-?[0-9.]+,-?[0-9.]+$")

    /** The longest base that prefixes [name] (the engine's resolve), or null. */
    fun baseOf(name: String): String? {
        var i = name.length
        while (i > 0) {
            val k = name.substring(0, i)
            if (k in BARE || k in PARAM || k in OPTIONAL || k == "area" || k == "mark") return k
            i = name.lastIndexOf(':', i - 1)
        }
        return null
    }

    fun valid(name: String): Boolean {
        if (name.startsWith("area:")) return AREA.matches(name)
        if (name.startsWith("mark:")) return name.length > "mark:".length
        val b = baseOf(name) ?: return false
        val param = if (name.length > b.length) name.substring(b.length + 1) else null
        return when {
            b in BARE -> param == null
            b in PARAM -> !param.isNullOrEmpty()
            b in OPTIONAL -> param == null || param.isNotEmpty()
            else -> false
        }
    }
}

object TourFlows {

    private fun m(kind: String, arg: String? = null) = TourMove(kind, arg)

    /** The seven flows, the prototype's order, titles and bodies from the one home of the texts. */
    fun all(tx: Tx): List<TourFlow> = listOf(phases(tx), survey(tx), existing(tx), wish(tx), vendor(tx), t360(tx), plan(tx))

    /** What the Help screen lists and the drawer row counts (the prototype's own `flows().length`). */
    fun shipped(tx: Tx): List<TourFlow> = all(tx).filter { it.ships }

    private fun phases(tx: Tx) = TourFlow(
        id = "phases", name = tx.tour_phases_name, line = tx.tour_phases_line, ships = false,
        missing = "phase:band — v54's phase band over survey prep (step 10): on the phone since port 62 slice L4, with no tour anchor on it yet; the flow's texts wait for prototype item P1", // text:value a blocked flow's missing-anchor note, developer-facing
        steps = listOf(
            TourStep("phases.1", "project", "phase:strip", "look",
                listOf(m("close"), m("go", "project"), m("phase", "0")),
                null, tx.tour_phases_1_title, tx.tour_phases_1_body),
            TourStep("phases.2", "project", "p54seg:0", "look",
                listOf(m("go", "project"), m("phase", "0")),
                null, tx.tour_phases_2_title, tx.tour_phases_2_body),
            TourStep("phases.3", "project", "p54seg:1", "do",
                listOf(m("go", "project"), m("phase", "0")),
                TourDone.Phase(1), tx.tour_phases_3_title, tx.tour_phases_3_body),
            TourStep("phases.4", "project", "chip:wish", "look",
                listOf(m("go", "project"), m("phase", "1")),
                null, tx.tour_phases_4_title, tx.tour_phases_4_body),
            // tour.phases.5.body is SKIPPED in the table ("prototype-only": its text says "the
            // prototype sends nothing" of itself), so this one step has no body until the phases
            // flow's phone wording is approved on the prototype — the flow is blocked regardless,
            // and the port invents no text (decision: a v61c tour is prototype work first).
            TourStep("phases.5", "project", "quote", "look",
                listOf(m("go", "project"), m("phase", "1")),
                null, tx.tour_phases_5_title, ""),
            TourStep("phases.6", "project", "p54seg:2", "look",
                listOf(m("go", "project"), m("phase", "1")),
                null, tx.tour_phases_6_title, tx.tour_phases_6_body),
            TourStep("phases.7", "project", "p54seg:3", "look",
                listOf(m("go", "project"), m("phase", "1")),
                null, tx.tour_phases_7_title, tx.tour_phases_7_body),
            TourStep("phases.8", "project", "p54seg:0", "do",
                listOf(m("go", "project"), m("phase", "1"), m("cancelAsk")),
                TourDone.Exists("phase:ask"), tx.tour_phases_8_title, tx.tour_phases_8_body),
            TourStep("phases.9", "project", "phase:ask", "do",
                listOf(m("go", "project"), m("phase", "1"), m("ask", "0")),
                TourDone.Gone("phase:ask"), tx.tour_phases_9_title, tx.tour_phases_9_body),
            TourStep("phases.10", "survey", "phase:band", "look",
                listOf(m("cancelAsk"), m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "mark")), // text:value the demo room's stored name, a position argument
                null, tx.tour_phases_10_title, tx.tour_phases_10_body),
        ))

    private fun survey(tx: Tx) = TourFlow(
        id = "survey", name = tx.tour_survey_name, line = tx.tour_survey_line, ships = true,
        steps = listOf(
            TourStep("survey.1", "clients", "row:Demo Client", "do", // text:value anchor name: the demo client row
                listOf(m("close"), m("go", "clients"), m("phase", "0")),
                TourDone.Screen("client"), tx.tour_survey_1_title, tx.tour_survey_1_body),
            TourStep("survey.2", "client", "row:Demo Site, Pune", "do", // text:value anchor name: the demo site row
                listOf(m("go", "client")),
                TourDone.Screen("site"), tx.tour_survey_2_title, tx.tour_survey_2_body),
            TourStep("survey.3", "site", "row:Demo flat interiors", "do", // text:value anchor name: the demo project row
                listOf(m("go", "site")),
                TourDone.Screen("project"), tx.tour_survey_3_title, tx.tour_survey_3_body),
            TourStep("survey.4", "project", "room:Guest Bedroom", "do", // text:value anchor name: the demo room tile
                listOf(m("go", "project")),
                TourDone.Screen("room"), tx.tour_survey_4_title, tx.tour_survey_4_body),
            TourStep("survey.5", "room", "capture360", "look",
                listOf(m("go", "room")),
                null, tx.tour_survey_5_title, tx.tour_survey_5_body),
            TourStep("survey.6", "room", "cap:360", "do",
                listOf(m("go", "room")),
                TourDone.Screen("survey"), tx.tour_survey_6_title, tx.tour_survey_6_body),
            TourStep("survey.7", "survey", "mode:lines", "look",
                listOf(m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "lines")), // text:value the demo room's stored name, a position argument
                null, tx.tour_survey_7_title, tx.tour_survey_7_body),
            TourStep("survey.8", "survey", "lines:row", "look",
                listOf(m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "lines")), // text:value the demo room's stored name, a position argument
                null, tx.tour_survey_8_title, tx.tour_survey_8_body),
            TourStep("survey.9", "survey", "tab:front", "look",
                listOf(m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "lines")), // text:value the demo room's stored name, a position argument
                null, tx.tour_survey_9_title, tx.tour_survey_9_body),
            TourStep("survey.10", "survey", "mode:mark", "do",
                listOf(m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "lines")), // text:value the demo room's stored name, a position argument
                TourDone.Mode("mark"), tx.tour_survey_10_title, tx.tour_survey_10_body),
            TourStep("survey.11", "survey", "mode:list", "do",
                listOf(m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "mark")), // text:value the demo room's stored name, a position argument
                TourDone.Mode("list"), tx.tour_survey_11_title, tx.tour_survey_11_body),
            TourStep("survey.12", "survey", "save", "look",
                listOf(m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "list")), // text:value the demo room's stored name, a position argument
                null, tx.tour_survey_12_title, tx.tour_survey_12_body),
        ))

    private fun existing(tx: Tx) = TourFlow(
        id = "existing", name = tx.tour_existing_name, line = tx.tour_existing_line, ships = false,
        missing = "more:electrical — the phone's More is a type grid (SpMoreSheet), no per-trade rows (step 7); chips — the phone's 3 · List is a checklist of rows, not the prototype's mark chips (step 9)", // text:value a blocked flow's missing-anchor note, developer-facing
        steps = listOf(
            TourStep("existing.1", "survey", "palette", "look",
                listOf(m("close"), m("phase", "0"), m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "mark"), m("tool", "sel"), m("more", "false")), // text:value the demo room's stored name, a position argument
                null, tx.tour_existing_1_title, tx.tour_existing_1_body),
            TourStep("existing.2", "survey", "tool:E", "do",
                listOf(m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "mark"), m("tool", "sel"), m("more", "false")), // text:value the demo room's stored name, a position argument
                TourDone.Tool("E"), tx.tour_existing_2_title, tx.tour_existing_2_body),
            TourStep("existing.3", "survey", "toolchip", "look",
                listOf(m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "mark"), m("tool", "E")), // text:value the demo room's stored name, a position argument
                null, tx.tour_existing_3_title, tx.tour_existing_3_body),
            TourStep("existing.4", "survey", "area:front:0.25,0.18,0.75,0.32", "do",
                listOf(m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "mark"), m("tool", "E")), // text:value the demo room's stored name, a position argument
                TourDone.Marks(1), tx.tour_existing_4_title, tx.tour_existing_4_body),
            TourStep("existing.5", "survey", "tool:sel", "do",
                listOf(m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "mark"), m("tool", "E")), // text:value the demo room's stored name, a position argument
                TourDone.Tool("sel"), tx.tour_existing_5_title, tx.tour_existing_5_body),
            TourStep("existing.6", "survey", "tool:more", "do",
                listOf(m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "mark"), m("tool", "sel"), m("more", "false")), // text:value the demo room's stored name, a position argument
                TourDone.Exists("more"), tx.tour_existing_6_title, tx.tour_existing_6_body),
            TourStep("existing.7", "survey", "more:electrical", "look",
                listOf(m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "mark"), m("tool", "sel"), m("more", "true")), // text:value the demo room's stored name, a position argument
                null, tx.tour_existing_7_title, tx.tour_existing_7_body),
            TourStep("existing.8", "survey", "selrow", "look",
                listOf(m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "mark"), m("tool", "sel"), m("more", "false"), m("sel", "FW1")), // text:value the demo room's stored name and a mark code, position arguments
                null, tx.tour_existing_8_title, tx.tour_existing_8_body),
            TourStep("existing.9", "survey", "chips", "look",
                listOf(m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "list"), m("pick", "FW1")), // text:value the demo room's stored name and a mark code, position arguments
                null, tx.tour_existing_9_title, tx.tour_existing_9_body),
            TourStep("existing.10", "survey", "reading:mm", "do",
                listOf(m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "list"), m("pick", "FW1")), // text:value the demo room's stored name and a mark code, position arguments
                null, tx.tour_existing_10_title, tx.tour_existing_10_body),
            TourStep("existing.11", "survey", "reading:d2", "look",
                listOf(m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "list"), m("pick", "FW1")), // text:value the demo room's stored name and a mark code, position arguments
                null, tx.tour_existing_11_title, tx.tour_existing_11_body),
            TourStep("existing.12", "survey", "show", "do",
                listOf(m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "list"), m("pick", "FW1")), // text:value the demo room's stored name and a mark code, position arguments
                TourDone.Tap, tx.tour_existing_12_title, tx.tour_existing_12_body),
            TourStep("existing.13", "survey", "lock", "do",
                listOf(m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "list"), m("pick", "FW1")), // text:value the demo room's stored name and a mark code, position arguments
                TourDone.Tap, tx.tour_existing_13_title, tx.tour_existing_13_body),
        ))

    private fun wish(tx: Tx) = TourFlow(
        id = "wish", name = tx.tour_wish_name, line = tx.tour_wish_line, ships = false,
        missing = "phase:band; the tool chip's as-Client-wish face (toolchip:as); the Mark sheet and its moves (sheet, sheet:move:wish, sheet:act, sheet:acts, sheet:done) — v53/v54 survey-prep lifecycle, on the phone since port 62 slices L3 and L4 with no tour anchor on any of it yet (steps 6-13)", // text:value a blocked flow's missing-anchor note, developer-facing
        steps = listOf(
            TourStep("wish.1", "project", "p54seg:1", "do",
                listOf(m("close"), m("go", "project"), m("phase", "0")),
                TourDone.Phase(1), tx.tour_wish_1_title, tx.tour_wish_1_body),
            TourStep("wish.2", "project", "chip:wish", "do",
                listOf(m("go", "project"), m("phase", "1")),
                TourDone.Screen("wish"), tx.tour_wish_2_title, tx.tour_wish_2_body),
            TourStep("wish.3", "wish", "wish:head", "look",
                listOf(m("phase", "1"), m("go", "wish")),
                null, tx.tour_wish_3_title, tx.tour_wish_3_body),
            TourStep("wish.4", "wish", "service:electrical", "do",
                listOf(m("phase", "1"), m("go", "wish")),
                TourDone.Tap, tx.tour_wish_4_title, tx.tour_wish_4_body),
            TourStep("wish.5", "wish", "service:row:master-bath", "look",
                listOf(m("phase", "1"), m("go", "wish")),
                null, tx.tour_wish_5_title, tx.tour_wish_5_body),
            TourStep("wish.6", "survey", "phase:band", "look",
                listOf(m("phase", "1"), m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "mark"), m("tool", "sel")), // text:value the demo room's stored name, a position argument
                null, tx.tour_wish_6_title, tx.tour_wish_6_body),
            TourStep("wish.7", "survey", "toolchip:as", "look",
                listOf(m("phase", "1"), m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "mark"), m("tool", "E")), // text:value the demo room's stored name, a position argument
                null, tx.tour_wish_7_title, tx.tour_wish_7_body),
            TourStep("wish.8", "survey", "area:front:0.25,0.18,0.75,0.32", "do",
                listOf(m("phase", "1"), m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "mark"), m("tool", "E")), // text:value the demo room's stored name, a position argument
                TourDone.Marks(1), tx.tour_wish_8_title, tx.tour_wish_8_body),
            TourStep("wish.9", "survey", "selrow:code", "do",
                listOf(m("close"), m("phase", "1"), m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "mark"), m("tool", "sel"), m("sel", "FW1")), // text:value the demo room's stored name and a mark code, position arguments
                TourDone.Exists("sheet"), tx.tour_wish_9_title, tx.tour_wish_9_body),
            TourStep("wish.10", "survey", "sheet:move:wish", "do",
                listOf(m("close"), m("phase", "1"), m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "mark"), m("tool", "sel"), m("sel", "FW1"), m("sheet", "FW1")), // text:value the demo room's stored name and a mark code, position arguments
                TourDone.Exists("sheet:act"), tx.tour_wish_10_title, tx.tour_wish_10_body),
            TourStep("wish.11", "survey", "sheet:acts", "do",
                listOf(m("sheet", "FW1")),
                null, tx.tour_wish_11_title, tx.tour_wish_11_body),
            TourStep("wish.12", "survey", "sheet:done", "do",
                listOf(m("sheet", "FW1")),
                TourDone.Gone("sheet"), tx.tour_wish_12_title, tx.tour_wish_12_body),
            TourStep("wish.13", "survey", "mark:FW1", "look",
                listOf(m("close"), m("phase", "1"), m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "mark"), m("tool", "sel")), // text:value the demo room's stored name, a position argument
                null, tx.tour_wish_13_title, tx.tour_wish_13_body),
        ))

    private fun vendor(tx: Tx) = TourFlow(
        id = "vendor", name = tx.tour_vendor_name, line = tx.tour_vendor_line, ships = false,
        missing = "VENDOR PACKS (phase:packs, pack:), the Trade chip (trade), State and its popover (list:state, list:pop), Move N (list:move), the Export sheet (list:export, x53, x53:trades) and the wishes seeding — v54/v58 survey-prep features the phone does not have", // text:value a blocked flow's missing-anchor note, developer-facing
        steps = listOf(
            TourStep("vendor.1", "project", "p54seg:2", "do",
                listOf(m("close"), m("go", "project"), m("phase", "1"), m("wishes", "FW1=replace,FE1=shift")),
                TourDone.Phase(2), tx.tour_vendor_1_title, tx.tour_vendor_1_body),
            TourStep("vendor.2", "project", "phase:packs", "look",
                listOf(m("go", "project"), m("phase", "2"), m("wishes", "FW1=replace,FE1=shift")),
                null, tx.tour_vendor_2_title, tx.tour_vendor_2_body),
            TourStep("vendor.3", "project", "pack:doors-windows", "look",
                listOf(m("go", "project"), m("phase", "2"), m("wishes", "FW1=replace,FE1=shift")),
                null, tx.tour_vendor_3_title, tx.tour_vendor_3_body),
            TourStep("vendor.4", "survey", "trade", "look",
                listOf(m("close"), m("phase", "2"), m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "mark"), m("tool", "sel"), m("wishes", "FW1=replace,FE1=shift")), // text:value the demo room's stored name, a position argument
                null, tx.tour_vendor_4_title, tx.tour_vendor_4_body),
            TourStep("vendor.5", "survey", "toolchip:as", "look",
                listOf(m("close"), m("phase", "2"), m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "mark"), m("tool", "E"), m("wishes", "FW1=replace,FE1=shift")), // text:value the demo room's stored name, a position argument
                null, tx.tour_vendor_5_title, tx.tour_vendor_5_body),
            TourStep("vendor.6", "survey", "area:front:0.25,0.18,0.75,0.32", "do",
                listOf(m("close"), m("phase", "2"), m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "mark"), m("tool", "E"), m("wishes", "FW1=replace,FE1=shift")), // text:value the demo room's stored name, a position argument
                TourDone.Marks(1), tx.tour_vendor_6_title, tx.tour_vendor_6_body),
            TourStep("vendor.7", "survey", "list:state", "do",
                listOf(m("close"), m("phase", "2"), m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "list"), m("states", null), m("wishes", "FW1=replace,FE1=shift")), // text:value the demo room's stored name, a position argument
                TourDone.Exists("list:pop"), tx.tour_vendor_7_title, tx.tour_vendor_7_body),
            TourStep("vendor.8", "survey", "list:pop", "do",
                listOf(m("phase", "2"), m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "list"), m("states", null), m("statePop"), m("wishes", "FW1=replace,FE1=shift")), // text:value the demo room's stored name, a position argument
                TourDone.Tap, tx.tour_vendor_8_title, tx.tour_vendor_8_body),
            TourStep("vendor.9", "survey", "list:move", "look",
                listOf(m("close"), m("phase", "2"), m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "list"), m("states", "wish"), m("wishes", "FW1=replace,FE1=shift")), // text:value the demo room's stored name, a position argument
                null, tx.tour_vendor_9_title, tx.tour_vendor_9_body),
            TourStep("vendor.10", "survey", "list:export", "do",
                listOf(m("close"), m("phase", "2"), m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "list"), m("states", null), m("export", ""), m("wishes", "FW1=replace,FE1=shift")), // text:value the demo room's stored name, a position argument
                TourDone.Exists("x53"), tx.tour_vendor_10_title, tx.tour_vendor_10_body),
            TourStep("vendor.11", "survey", "x53:trades", "look",
                listOf(m("phase", "2"), m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "list"), m("states", null), m("wishes", "FW1=replace,FE1=shift"), m("export", "vendor:doors-windows")), // text:value the demo room's stored name, a position argument
                null, tx.tour_vendor_11_title, tx.tour_vendor_11_body),
            TourStep("vendor.12", "capture", "stg", "look",
                listOf(m("close"), m("phase", "2"), m("details", "Guest Bedroom"), m("wishes", "FW1=replace,FE1=shift")), // text:value the demo room's stored name, a position argument
                null, tx.tour_vendor_12_title, tx.tour_vendor_12_body),
        ))

    private fun t360(tx: Tx) = TourFlow(
        id = "360", name = tx.tour_360_name, line = tx.tour_360_line, ships = false,
        missing = "the prototype's 360 TAB (tab:360 — the phone's tab rows are StepRoute.TAB_ROWS, no 360) and its strip-less 360 view: the phone's 360 is strip step 4 (StepRoute.STEPS), so steps 1, 3 and 7's words do not run true here (§31 critic finding 1)", // text:value a blocked flow's missing-anchor note, developer-facing
        steps = listOf(
            TourStep("360.1", "survey", "tab:360", "do",
                listOf(m("close"), m("phase", "0"), m("survey", "Guest Bedroom"), m("tab", "front"), m("mode", "mark"), m("tool", "sel")), // text:value the demo room's stored name, a position argument
                TourDone.Tab("360"), tx.tour_360_1_title, tx.tour_360_1_body),
            TourStep("360.2", "survey", "pano", "do",
                listOf(m("survey", "Guest Bedroom"), m("tab", "360"), m("tool", "sel")), // text:value the demo room's stored name, a position argument
                null, tx.tour_360_2_title, tx.tour_360_2_body),
            TourStep("360.3", "survey", "mode", "look",
                listOf(m("survey", "Guest Bedroom"), m("tab", "360"), m("tool", "sel")), // text:value the demo room's stored name, a position argument
                null, tx.tour_360_3_title, tx.tour_360_3_body),
            TourStep("360.4", "survey", "palette", "look",
                listOf(m("survey", "Guest Bedroom"), m("tab", "360"), m("tool", "sel")), // text:value the demo room's stored name, a position argument
                null, tx.tour_360_4_title, tx.tour_360_4_body),
            TourStep("360.5", "survey", "tool:E", "do",
                listOf(m("survey", "Guest Bedroom"), m("tab", "360"), m("tool", "sel")), // text:value the demo room's stored name, a position argument
                TourDone.Tool("E"), tx.tour_360_5_title, tx.tour_360_5_body),
            TourStep("360.6", "survey", "area:front:0.25,0.18,0.75,0.32", "do",
                listOf(m("survey", "Guest Bedroom"), m("tab", "360"), m("look", "front"), m("tool", "E")), // text:value the demo room's stored name, a position argument
                TourDone.Marks(1), tx.tour_360_6_title, tx.tour_360_6_body),
            TourStep("360.7", "survey", "tab:front", "look",
                listOf(m("survey", "Guest Bedroom"), m("tab", "360"), m("tool", "sel")), // text:value the demo room's stored name, a position argument
                null, tx.tour_360_7_title, tx.tour_360_7_body),
        ))

    private fun plan(tx: Tx) = TourFlow(
        id = "plan", name = tx.tour_plan_name, line = tx.tour_plan_line, ships = false,
        missing = "the project-level Floor plan arranger (plan:tile, plan:room, plan:rotate, plan:fit, plan:reset) does not exist on the phone; SurveyPlanScreen is the per-room PLAN tab, a different thing", // text:value a blocked flow's missing-anchor note, developer-facing
        steps = listOf(
            TourStep("plan.1", "project", "plan:tile", "do",
                listOf(m("close"), m("go", "project")),
                TourDone.Screen("plan"), tx.tour_plan_1_title, tx.tour_plan_1_body),
            TourStep("plan.2", "plan", "plan:room:Guest Bedroom", "do", // text:value anchor name: the demo room on the floor plan
                listOf(m("go", "plan")),
                null, tx.tour_plan_2_title, tx.tour_plan_2_body),
            TourStep("plan.3", "plan", "plan:rotate", "do",
                listOf(m("go", "plan"), m("planSel", "Guest Bedroom")), // text:value the demo room's stored name, a position argument
                TourDone.Tap, tx.tour_plan_3_title, tx.tour_plan_3_body),
            TourStep("plan.4", "plan", "plan:room:Guest Bedroom", "look", // text:value anchor name: the demo room on the floor plan
                listOf(m("go", "plan"), m("planSel", "Guest Bedroom")), // text:value the demo room's stored name, a position argument
                null, tx.tour_plan_4_title, tx.tour_plan_4_body),
            TourStep("plan.5", "plan", "plan:fit", "do",
                listOf(m("go", "plan")),
                TourDone.Tap, tx.tour_plan_5_title, tx.tour_plan_5_body),
            TourStep("plan.6", "plan", "plan:reset", "look",
                listOf(m("go", "plan")),
                null, tx.tour_plan_6_title, tx.tour_plan_6_body),
        ))
}
