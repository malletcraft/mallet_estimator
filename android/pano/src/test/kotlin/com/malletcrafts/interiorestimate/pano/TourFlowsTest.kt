package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The seven flows as the prototype ships them (port 61c §31): the counts, the anchor
 * vocabulary, both languages' texts, the Back-lands-right rule (a `done` step's setup makes
 * its own predicate false), and the shipped set — which this test, not a comment, keeps
 * honest. The flow data was transcribed from index61c2.html's `window.T55_FLOWS` and the
 * driven run of 9 Nov 2026 (146 steps, EN and मराठी, every anchor found).
 *
 * THE HELP TOURS ARE OFF since port 63 slice M2 (Features.HELP_TOURS; Amit, 15 Nov 2026: "For time
 * being hold help tour as well from development as it can be done once I approve survey to quote
 * journey its functionality etc."), in the same build that changes the measuring screen the survey
 * tour teaches. NOTHING IS DELETED: the shipped set is asked here with the tours ON, stated on
 * every call, so the flow that shipped is held exactly as it was; with them off it is empty.
 */
class TourFlowsTest {

    private val en = TourFlows.all(Tx.EN)
    private val mr = TourFlows.all(Tx.MR)

    @Test fun `seven flows, 73 steps, the prototype's order and counts`() {
        assertEquals(listOf("phases", "survey", "existing", "wish", "vendor", "360", "plan"), en.map { it.id })
        assertEquals(listOf(10, 12, 13, 13, 12, 7, 6), en.map { it.steps.size })
        assertEquals(73, en.sumOf { it.steps.size })
    }

    @Test fun `the shipped set is the survey flow alone - 1 tour, 12 steps (the 360 is blocked by the missing 360 tab, §31 critic finding 1)`() {
        val s = TourFlows.shipped(Tx.EN, tours = true)
        assertEquals(listOf("survey"), s.map { it.id })
        assertEquals(12, s.single().steps.size)
        // every blocked flow names what is missing, so flipping it later starts from a list
        for (f in en.filter { !it.ships }) assertNotNull(f.missing, f.id)
        assertNull(en.single { it.ships }.missing)
    }

    @Test fun `step ids are flow-dot-n, 1-based, in order`() {
        for (f in en) f.steps.forEachIndexed { i, st ->
            assertEquals("${f.id}.${i + 1}", st.id)
            assertTrue(st.mode == "do" || st.mode == "look", st.id)
            assertTrue(st.screen.isNotBlank(), st.id)
        }
    }

    @Test fun `every anchor parses against the vocabulary, done anchors included`() {
        for (f in en) for (st in f.steps) {
            st.anchor?.let { assertTrue(TourAnchors.valid(it), "${st.id}: $it") }
            when (val d = st.done) {
                is TourDone.Exists -> assertTrue(TourAnchors.valid(d.anchor), "${st.id}: exists ${d.anchor}")
                is TourDone.Gone -> assertTrue(TourAnchors.valid(d.anchor), "${st.id}: gone ${d.anchor}")
                else -> {}
            }
        }
        // the grammar still refuses what it should
        for (bad in listOf("", "row", "tab", "frob", "save:x", "area:front:1,2,3", "mark:"))
            assertFalse(TourAnchors.valid(bad), bad)
    }

    @Test fun `no shipped step names a virtual anchor - this build computes none`() {
        for (f in TourFlows.shipped(Tx.EN, tours = true)) for (st in f.steps) {
            val a = st.anchor ?: continue
            assertFalse(a.startsWith("area:") || a.startsWith("mark:"), st.id)
        }
    }

    @Test fun `every title and body is non-blank in BOTH languages, and every flow's name and line`() {
        // The ONE exception, pinned: tour.phases.5.body is skipped in the table ("prototype-only"
        // — its text describes the prototype itself), so that step's body stays empty until the
        // phases flow's phone wording is approved on the prototype. The flow is blocked anyway.
        for (flows in listOf(en, mr)) for (f in flows) {
            assertTrue(f.name.isNotBlank(), f.id)
            assertTrue(f.line.isNotBlank(), f.id)
            for (st in f.steps) {
                assertTrue(st.title.isNotBlank(), st.id)
                if (st.id == "phases.5") assertTrue(st.body.isEmpty(), "phases.5 gains a body only through the table")
                else assertTrue(st.body.isNotBlank(), st.id)
            }
        }
        // and मराठी really is मराठी where the step was translated: the two tables differ
        assertTrue(en.zip(mr).all { (a, b) -> a.name != b.name || a.steps.zip(b.steps).any { (x, y) -> x.title != y.title } })
    }

    @Test fun `the first step of every flow opens with the everything-goes-back sentence`() {
        for (f in en) assertTrue("back at the end" in f.steps[0].body, f.id)
        for (f in mr) assertTrue("शेवटी" in f.steps[0].body, f.id)
    }

    @Test fun `a done step's setup makes its own predicate false, so Back never lands on a step that advances at once`() {
        for (f in en) for (st in f.steps) {
            fun setupArg(kind: String): String? = st.setup.lastOrNull { it.kind == kind }?.arg
            when (val d = st.done) {
                is TourDone.Mode -> assertTrue(setupArg("mode") != d.mode, st.id)
                is TourDone.Tool -> assertTrue(setupArg("tool") != d.tool, st.id)
                is TourDone.Tab -> assertTrue(setupArg("tab") != d.tab, st.id)
                is TourDone.Phase -> assertTrue(setupArg("phase") != d.n.toString(), st.id)
                else -> {}
            }
        }
    }

    @Test fun `the shipped survey flow uses only the moves the tour host executes`() {
        val hosted = setOf("close", "go", "phase", "survey", "tab", "mode")
        for (st in TourFlows.shipped(Tx.EN, tours = true).single().steps)
            for (mv in st.setup) assertTrue(mv.kind in hosted, "${st.id}: ${mv.kind}")
    }

    @Test fun `with the help tours off nothing ships - and off is the default the Help screen and the drawer's row read (port 63 slice M2)`() {
        for (tx in listOf(Tx.EN, Tx.MR)) {
            assertEquals(emptyList(), TourFlows.shipped(tx, tours = false))
            assertEquals(TourFlows.shipped(tx, tours = Features.HELP_TOURS).map { it.id }, TourFlows.shipped(tx).map { it.id }, "the default is the switch")
        }
        assertEquals(0, TourFlows.shipped(Tx.EN).size, "the drawer's row would read 0 tours: it is not drawn (FeaturesTest pins the gate)")
        // the flows themselves are untouched by the switch: all seven, the survey flow still the one that would ship
        assertEquals(7, TourFlows.all(Tx.EN).size); assertEquals(listOf("survey"), TourFlows.all(Tx.EN).filter { it.ships }.map { it.id })
        // ... and its step 11 still teaches the list, a step the phone no longer has: the reason the tour is off, and what
        // whoever rewrites it after Amit approves the journey starts from
        val eleven = TourFlows.all(Tx.EN).single { it.id == "survey" }.steps[10]
        assertEquals("mode:list", eleven.anchor); assertEquals(TourDone.Mode("list"), eleven.done)
        assertTrue("list" !in StepRoute.ROUTE)
    }
}
