package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.SaveStay.Effect
import com.malletcrafts.interiorestimate.pano.i18n.Msg
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ✓ SAVE SAVES AND STAYS (port 63 slice I1c; DESIGN-imagemeter §21, P2). Amit, after using 0.3.345: "Click on save always
 * take me out to room foto. its annoying." Every effect of a save happens exactly once a press, survey prep stays open,
 * the bar says what the save did for as long as nothing has changed since, and ← after a save repeats nothing.
 *
 * (Test names are plain ASCII: MeasureWalkTest's header says why.)
 */
class SaveStayTest {

    private val auto: Msg = { tx: Tx -> tx.sp_msg_saved + "14:05" }

    @Test fun `a press does every effect of a save, each once, in order - and leaving is not one of them`() {
        assertEquals(listOf(Effect.SAVE, Effect.UPLOAD, Effect.KEEP_SIZE, Effect.PICTURES), SaveStay.effects(6, readBack = true))
        // a save that did not read back queues no upload -- the web never gets a prep this phone does not hold -- and the
        // room size and the pictures still follow, as today's save callback runs them
        assertEquals(listOf(Effect.SAVE, Effect.KEEP_SIZE, Effect.PICTURES), SaveStay.effects(6, readBack = false))
        // before the save has answered, only the save
        assertEquals(listOf(Effect.SAVE), SaveStay.effects(6, readBack = null))
        // each effect at most once a press
        for (rb in listOf(true, false, null)) SaveStay.effects(6, rb).let { assertEquals(it.distinct(), it) }
        // Save's own rule is unchanged: all six surfaces bounded, or nothing at all
        for (n in 0..5) for (rb in listOf(true, false, null)) assertTrue(SaveStay.effects(n, rb).isEmpty(), "$n bounded")
        assertTrue(SaveStay.may(6)); assertFalse(SaveStay.may(5)); assertEquals(6, SurveyPrep.SURFACES.size)
        assertEquals(listOf("SAVE", "UPLOAD", "KEEP_SIZE", "PICTURES"), Effect.values().map { it.name }, "no Effect leaves survey prep")
    }

    @Test fun `after a save the bar says what the save did, until the next change - then the autosave's own line`() {
        var m = SaveStay.OPENED
        // never saved: the autosave's line, as the bar shows it today
        assertEquals("Saved ✓ 14:05", SaveStay.bar(Tx.EN, m, 0, auto)); assertFalse(SaveStay.changedSince(m, 7))
        // a press at change count 7 that read back: writing its pictures
        m = SaveStay.saved(m, 7, readBack = true)
        assertEquals("Survey prep saved — writing its pictures…", SaveStay.bar(Tx.EN, m, 7, auto))
        // the pictures written: how many
        m = SaveStay.written(m, m.seq, 6)
        assertEquals("Survey prep saved · 6 pictures in the room folder", SaveStay.bar(Tx.EN, m, 7, auto))
        // ONE change after it: the autosave's line, never "pictures written" of a room that has changed since
        assertEquals("Saved ✓ 14:05", SaveStay.bar(Tx.EN, m, 8, auto)); assertTrue(SaveStay.changedSince(m, 8))
        // pictures not written: the reason, in the system's own words
        val n = SaveStay.notWritten(SaveStay.saved(m, 8, true), m.seq + 1, "disk full")
        assertEquals("Survey prep saved on this phone; pictures not written: disk full", SaveStay.bar(Tx.EN, n, 8, auto))
        // a save that did not read back says so
        assertEquals("Not saved: did not read back", SaveStay.bar(Tx.EN, SaveStay.saved(m, 9, readBack = false), 9, auto))
        // in the language: the same keys the room screen's result line has always used
        assertEquals("सर्व्हे तयारी सेव्ह झाली · खोलीच्या फोल्डरमध्ये 6 चित्रे", SaveStay.bar(Tx.MR, m, 7, auto))
    }

    @Test fun `two presses that overlap - only the newest press's answer is shown`() {
        val first = SaveStay.saved(SaveStay.OPENED, 3, true)
        val second = SaveStay.saved(first, 3, true)
        assertEquals(first.seq + 1, second.seq)
        // the first press's rewrite ends after the second began: its count does not overwrite the newer "writing…"
        val late = SaveStay.written(second, first.seq, 5)
        assertEquals(second, late); assertEquals("Survey prep saved — writing its pictures…", SaveStay.bar(Tx.EN, late, 3, auto))
        assertEquals(second, SaveStay.notWritten(second, first.seq, "x"))
        // the second press's own answer is
        assertEquals("Survey prep saved · 6 pictures in the room folder", SaveStay.bar(Tx.EN, SaveStay.written(second, second.seq, 6), 3, auto))
    }

    @Test fun `back after a save repeats nothing of it, and says whether anything was changed since`() {
        val m = SaveStay.written(SaveStay.saved(SaveStay.OPENED, 4, true), 1, 6)
        SaveStay.leave(m, 4).let { assertTrue(it.repeats.isEmpty()); assertTrue(it.then.isEmpty()); assertFalse(it.changedSince) }
        // a change after the last Save: kept on the phone, and it goes to the web and the pictures with the next Save
        SaveStay.leave(m, 5).let { assertTrue(it.repeats.isEmpty()); assertTrue(it.changedSince) }
        // never saved: back is what it always was
        SaveStay.leave(SaveStay.OPENED, 9).let { assertTrue(it.repeats.isEmpty()); assertTrue(it.then.isEmpty()); assertFalse(it.changedSince) }
        // a KEPT capture's leave runs nothing after it
        assertTrue(SaveStay.leave(m, 4).then.isEmpty()); assertTrue(SaveStay.leave(m, 5).then.isEmpty())
    }

    @Test fun `a new capture - a press saves alone, and the keep on back files it and then queues the upload the press held back, once`() {
        // ✓ Save and ← both FILE a new capture today (keepWith): staying, a press is the read-back save alone -- its
        // upload would be refused after five tries if it ran before the capture's row exists
        assertEquals(listOf(Effect.SAVE), SaveStay.effects(6, true, SaveStay.Flow.NEW)); assertEquals(listOf(Effect.SAVE), SaveStay.effects(6, false, SaveStay.Flow.NEW))
        assertTrue(SaveStay.effects(5, true, SaveStay.Flow.NEW).isEmpty())
        // the keep on ← then queues it, once -- only after a press that read back
        val m = SaveStay.saved(SaveStay.OPENED, 2, readBack = true)
        assertEquals(listOf(Effect.UPLOAD), SaveStay.leave(m, 2, SaveStay.Flow.NEW).then)
        assertEquals(listOf(Effect.UPLOAD), SaveStay.leave(SaveStay.saved(m, 3, true), 3, SaveStay.Flow.NEW).then, "two presses: still one upload")
        assertTrue(SaveStay.leave(SaveStay.saved(m, 3, false), 3, SaveStay.Flow.NEW).then.isEmpty(), "the last press did not read back")
        assertTrue(SaveStay.leave(SaveStay.OPENED, 0, SaveStay.Flow.NEW).then.isEmpty(), "no press: ← keeps it, as it always has")
        assertTrue(SaveStay.leave(m, 2, SaveStay.Flow.NEW).repeats.isEmpty())
    }

    @Test fun `a save touches nothing that is picked, held or gold - survey prep stays on the same face, step and pick`() {
        // the save is not an event of Pick: no function of it takes or returns a Pick.State, so nothing the screen holds
        // -- the face, the step, the zoom, the pick, the gold -- can be changed by it
        for (f in SaveStay::class.java.declaredMethods) {
            assertFalse(f.parameterTypes.any { it == Pick.State::class.java }, f.name)
            assertFalse(f.returnType == Pick.State::class.java, f.name)
            assertFalse(f.returnType == String::class.java && f.name.startsWith("open"), f.name)
        }
    }
}
