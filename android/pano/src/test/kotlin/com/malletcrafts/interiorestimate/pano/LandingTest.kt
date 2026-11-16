package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.Landing.Haptic
import com.malletcrafts.interiorestimate.pano.Pick.Pad
import com.malletcrafts.interiorestimate.pano.Pick.State
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A LASER READING ARRIVES (port 63 slice I1a; DESIGN-imagemeter §4.3, §4.4): where it goes, whether the gold then
 * moves, what the hand feels and what the note says. The meter's own button takes the reading, with his eyes on the
 * wall, so every one of these is held here -- the screen is compiled only by CI. The synthetic room of MeasTest.
 *
 * (Test names are plain ASCII: MeasureWalkTest's header says why.)
 */
class LandingTest {

    private val W = 2678.0; private val H = 2677.0; private val L = 3319.0
    private fun ab(s: String): DoubleArray = SurveyPrep.size(s, L, W, H)
    private val SURVEY = PhaseTools.SURVEY
    private val D1 = "id-front-D1"; private val W1 = "id-front-W1"

    private fun SurveyPrep.Prep.put(s: String, t: String, n: Int, x0: Double, x1: Double, y0: Double, y1: Double): SurveyPrep.Mark =
        SurveyPrep.Mark("id-$s-$t$n", t, SurveyPrep.CODE.getValue(s) + t + n, x0, x1, y0, y1, emptyMap()).also { surface(s).marks.add(it) }

    /** A door and a window on the front wall. */
    private fun wall(): SurveyPrep.Prep {
        val p = SurveyPrep.fresh(L, W, H)
        p.put("front", "D", 1, 300.0, 1200.0, 0.0, 2100.0)
        p.put("front", "W", 1, 1500.0, 2400.0, 900.0, 2100.0)
        return p
    }
    private val DOOR = listOf("FD1.w", "FD1.h", "FD1.l", "FD1.p")
    private val WINDOW = listOf("FW1.w", "FW1.h", "FW1.r", "FW1.s", "FW1.p")

    private fun sc(p: SurveyPrep.Prep, stage: Int? = SURVEY, step: String = StepRoute.MARK) = Pick.Scene(p, stage, step, ::ab)
    private fun refused(r: Landing.Route): Landing.Route.Refused = assertIs<Landing.Route.Refused>(r)

    /** RefFrameTest's reference: a 905 x 2053 door on the front wall, placed and on. */
    private fun withRef(p: SurveyPrep.Prep) {
        val f = assertNotNull(SurveyPrep.laserFrame("front", p, L, W, H))
        val q = listOf(doubleArrayOf(300.0, 0.0), doubleArrayOf(1205.0, 0.0), doubleArrayOf(1205.0, 2053.0), doubleArrayOf(300.0, 2053.0)).map { SurveyPrep.apply(f.m2p, it) }
        val m = linkedMapOf<String, Any?>("on" to true, "q" to q.map { listOf(it[0], it[1]) }, "w" to null, "h" to null)
        p.surface("front").extra = SurveyPrep.withKey(p.surface("front").extra, SurveyRef.KEY, m)
    }

    // ------------------------------------------------------------------ where it goes

    @Test fun `a reading lands on the gold figure - nothing else is asked, a locked mark's figure takes it too`() {
        val p = wall()
        assertEquals("FD1.w", assertIs<Landing.Route.Gold>(Landing.route(null, sc(p), State(D1, true, "FD1.w"), 898, 0L)).code)
        assertEquals("FW1.s", assertIs<Landing.Route.Gold>(Landing.route(null, sc(p), State(W1, false, "FW1.s"), 912, 0L)).code)
        // unread stage: readings are keyed as at Survey
        assertIs<Landing.Route.Gold>(Landing.route(null, sc(p, null), State(D1, false, "FD1.h"), 2095, 0L))
        // our lock holds a mark's position, not its figures
        assertEquals(true, SurveyPrepEdit.setLocked(p, D1, true))
        assertIs<Landing.Route.Gold>(Landing.route(null, sc(p), State(D1, false, "FD1.w"), 898, 0L))
    }

    @Test fun `a reading with nothing gold is refused, said, not stored - felt as one long buzz, its note staying until the next touch`() {
        val p = wall()
        for (st in listOf(Pick.NONE, State(D1, true, null), State(D1, false, "FD1.zz"), State(D1, false, "FZ9.w"))) {
            val r = refused(Landing.route(null, sc(p), st, 1203, 0L))
            assertEquals("D2 1203 mm — tap a measurement first, then read again", r.text(Tx.EN)); assertEquals(Haptic.LONG, r.haptic); assertTrue(r.stays)
        }
        // in 1 · Lines with no reference box armed: the same refusal, as today
        assertEquals("D2 905 mm — tap a measurement first, then read again", refused(Landing.route(null, sc(p, SURVEY, StepRoute.LINES), Pick.NONE, 905, 0L)).text(Tx.EN))
        assertTrue(p.actual.isEmpty(), "route writes nothing")
    }

    @Test fun `on the 360 a reading is refused in its own words, and after Survey in the phase's - the phase's first`() {
        val p = wall(); val st = State(D1, false, "FD1.w")
        refused(Landing.route(null, sc(p, SURVEY, StepRoute.PANO), st, 1203, 0L)).let {
            assertEquals("D2 1203 mm — not used: readings are taken on the wall's own tab", it.text(Tx.EN)); assertEquals(Haptic.LONG, it.haptic); assertTrue(it.stays) }
        for (stage in listOf(1, 2, 3, 4, 5)) for (step in StepRoute.ROUTE) refused(Landing.route(null, sc(p, stage, step), st, 1203, 0L)).let {
            assertEquals("Readings are taken in the Survey phase — move the phase back on the project screen to key one", it.text(Tx.EN), "$step at $stage")
            assertEquals(Haptic.LONG, it.haptic); assertTrue(it.stays) }
    }

    @Test fun `a reference box takes its reading as before, and one that was left sets it aside`() {
        val p = wall(); withRef(p); val t0 = 1_000_000L; val st = State(D1, false, "FD1.w")
        // armed in 1 · Lines: into that box, never onto the gold figure
        val shot = SurveyRef.arm("front", "w", t0)
        assertIs<Landing.Route.Ref>(Landing.route(shot, sc(p, SURVEY, StepRoute.LINES), Pick.NONE, 905, t0 + 800)).let { assertEquals("front", it.s); assertEquals("w", it.k) }
        // the box was left for 2 · Mark and its reading arrives within five seconds: set aside, felt, never a mark's
        val left = SurveyRef.leave(shot)
        refused(Landing.route(left, sc(p), st, 2053, t0 + 1200)).let {
            assertEquals("D2 2053 mm — not used: the reference box was left", it.text(Tx.EN)); assertEquals(Haptic.LONG, it.haptic); assertTrue(it.stays) }
        // a stale shot takes nothing: the gold figure's
        assertEquals("FD1.w", assertIs<Landing.Route.Gold>(Landing.route(left, sc(p), st, 898, t0 + SurveyRef.SHOT_MS + 1)).code)
    }

    // ------------------------------------------------------------------ what follows a reading that is in

    @Test fun `a reading that passes - one tick, the keypad shut with its digits dropped, the plate rings, the gold walks on`() {
        val p = wall(); p.actual["FD1.w"] = 898.0
        val a = Landing.after(sc(p), State(D1, true, "FD1.w"), "FD1.w", busy = false)
        assertEquals(State(D1, false, "FD1.h"), a.state); assertEquals(Haptic.TICK, a.haptic); assertEquals(Pad.SHUT_DROP, a.pad)
        assertEquals("FD1.w", a.ring); assertTrue(a.byItself); assertFalse(a.waits); assertNull(a.note)
        assertEquals(1000L, Landing.RING_MS)
        // onto the next mark of the wall, whose plate may be off screen: the photo pans only then (byItself says to ask)
        for (k in DOOR) p.actual[k] = p.readingOf(k, ::ab)!!.second.est ?: 100.0
        Landing.after(sc(p), State(D1, false, "FD1.p"), "FD1.p", busy = false).let { assertEquals(State(W1, false, "FW1.w"), it.state); assertTrue(it.byItself) }
    }

    @Test fun `a reading that fails the check does not move on - a double buzz, the plate keeps its mark, the note says so with no figure`() {
        val p = wall(); p.actual["FD1.w"] = 2100.0
        val a = Landing.after(sc(p), State(D1, true, "FD1.w"), "FD1.w", busy = false)
        assertEquals(State(D1, false, "FD1.w"), a.state); assertEquals(Haptic.DOUBLE, a.haptic); assertFalse(a.byItself); assertEquals("FD1.w", a.ring)
        val said = a.note!!(Tx.EN)
        assertEquals("This reading does not match the mark. Shoot again, or ▸ to keep it.", said)
        // NO FIGURE: not the reading, not what the photo says, no ≈
        assertFalse(said.any { it.isDigit() }); assertFalse("≈" in said)
        // a less-than-half reading the same: the door's 2100 height read as 905
        p.actual["FD1.h"] = 905.0
        assertEquals(Haptic.DOUBLE, Landing.after(sc(p), State(D1, false, "FD1.h"), "FD1.h", busy = false).haptic)
        // the three haptics are three different things
        assertEquals(3, Haptic.values().toSet().size)
    }

    @Test fun `the wall's last reading - nothing is gold and the note says the wall, or the room, is finished`() {
        val p = wall(); p.put("left", "W", 1, 500.0, 1500.0, 900.0, 2100.0)
        for (k in DOOR + WINDOW) p.actual[k] = p.readingOf(k, ::ab)!!.second.est ?: 100.0
        Landing.after(sc(p), State(W1, false, "FW1.p"), "FW1.p", busy = false).let { a ->
            assertEquals(State(W1, false, null), a.state); assertEquals(Haptic.TICK, a.haptic); assertFalse(a.byItself)
            assertEquals("Front wall · all 9 measured ✓", a.note!!(Tx.EN))
        }
        for (k in ab("left").let { MeasureWalk.stops(p, "left", it[0], it[1]) }) p.actual[k] = 1000.0
        assertEquals("Room measured · all 14 ✓", Landing.after(sc(p), State(W1, false, "FW1.p"), "FW1.p", busy = false).note!!(Tx.EN))
        // the face never changes by itself: nothing here names another face
    }

    @Test fun `with a sheet open or a finger on a grab the reading is in and felt, and the gold waits for the release`() {
        val p = wall(); p.actual["FD1.w"] = 898.0; val st = State(D1, true, "FD1.w")
        val a = Landing.after(sc(p), st, "FD1.w", busy = true)
        assertEquals(st, a.state); assertTrue(a.waits); assertEquals(Haptic.TICK, a.haptic); assertEquals("FD1.w", a.ring); assertFalse(a.byItself)
        // the sheet closes, or the finger lifts: the gold moves now -- nothing is felt again, nothing rings again
        Landing.released(sc(p), st, a.ring!!).let { r ->
            assertEquals(State(D1, false, "FD1.h"), r.state); assertNull(r.haptic); assertNull(r.ring); assertTrue(r.byItself); assertFalse(r.waits); assertEquals(Pad.KEEP, r.pad) }
        // a reading that fails, landed under a sheet: felt as a failure at once, said at once, and it stays on release
        p.actual["FD1.w"] = 2100.0
        Landing.after(sc(p), st, "FD1.w", busy = true).let { assertEquals(Haptic.DOUBLE, it.haptic); assertNotNull(it.note); assertTrue(it.waits) }
        assertEquals(State(D1, false, "FD1.w"), Landing.released(sc(p), st, "FD1.w").state)
    }

    @Test fun `the keypad's Next stores a typed figure and the gold moves - the keypad stays up, nothing is felt, a failed check is said and still moves`() {
        val p = wall(); p.actual["FD1.w"] = 898.0
        Landing.keyed(sc(p), State(D1, true, "FD1.w"), "FD1.w").let { a ->
            assertEquals(State(D1, false, "FD1.h"), a.state); assertEquals(Pad.KEEP, a.pad); assertNull(a.haptic); assertNull(a.ring); assertNull(a.note); assertTrue(a.byItself) }
        // A TYPED FIGURE IS DELIBERATE: it keeps its `!`, the note says so, and the gold still moves
        p.actual["FD1.h"] = 905.0
        Landing.keyed(sc(p), State(D1, false, "FD1.h"), "FD1.h").let { a ->
            assertEquals(State(D1, false, "FD1.l"), a.state); assertEquals("This reading does not match the mark. Shoot again, or ▸ to keep it.", a.note!!(Tx.EN)) }
        assertTrue(MeasureFlow.flagged(p, "FD1.h", ::ab))
    }
}
