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
        assertEquals(listOf("SAVE", "UPLOAD", "KEEP_SIZE", "PICTURES", "FILE"), Effect.values().map { it.name }, "no Effect leaves survey prep")
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

    // ---- port 63 P2N: ✓ Save on a brand-new capture files it on the press, once, and stays --------------------------------

    /**
     * THE THREE PLACES THAT RUN A SAVE, AS :app HAS THEM, with every effect written down as it runs: the screen at the
     * press (`SurveyPrepScreen.saveNow`), the caller's keep ONE AT A TIME in the order it was asked (`MainActivity`'s
     * `keepNew`, a Mutex taken from the main thread: [turn] lets the next one in), and the screen again at the keep's
     * answer; and ← (`back`). Each asks SaveStay and does what it answers -- nothing here decides anything.
     * [fails]: how many filings fail before one succeeds (the disk is full, say).
     */
    private class Phone(filed: Boolean, private var fails: Int = 0) {
        var mem = SaveStay.opened(filed)
        /** the capture's row exists (and so do its faces) */
        var row = filed
        var rev = 0
        var open = true
        val ran = ArrayList<String>()
        private val lock = ArrayDeque<() -> Unit>()

        fun press(readBack: Boolean = true, bounded: Int = 6) {
            check(open) { "survey prep is closed" }
            val fx = SaveStay.effects(bounded, readBack, SaveStay.flow(mem))
            if (fx.isEmpty()) return
            ran += "SAVE"
            mem = SaveStay.saved(mem, rev, readBack)
            val seq = mem.seq
            if (Effect.UPLOAD in fx) { check(row) { "an upload queued at a press with no row" }; ran += "UPLOAD" }
            if (SaveStay.toKeep(fx)) lock += {
                keep(leaving = false) { n, why ->
                    val a = SaveStay.answered(mem, seq, n, why)
                    mem = a.mem
                    if (Effect.UPLOAD in a.then) { check(row) { "an upload queued at an answer with no row" }; ran += "UPLOAD" }
                }
            }
        }

        fun change() { check(open); rev++ }

        fun back() {
            check(open)
            val l = SaveStay.leave(mem, rev)
            mem = SaveStay.left(mem)
            open = false
            assertTrue(l.repeats.isEmpty())
            lock += { keep(leaving = true, report = null) }
            if (Effect.UPLOAD in l.then) ran += "UPLOAD"
        }

        private fun keep(leaving: Boolean, report: ((Int?, String?) -> Unit)?) {
            val fx = SaveStay.keep(filed = row, leaving = leaving)
            if (Effect.FILE in fx) {
                if (fails > 0) { fails--; ran += "FILE failed"; report?.invoke(null, "disk full") }
                else { row = true; ran += "FILE"; report?.invoke(13, null) }
            }
            if (Effect.KEEP_SIZE in fx) { check(row); ran += "KEEP_SIZE" }
            if (Effect.PICTURES in fx) { check(row); ran += "PICTURES"; report?.invoke(6, null) }
        }

        /** The keep takes its next turn, if one is waiting. */
        fun turn(): Boolean { val k = lock.removeFirstOrNull() ?: return false; k(); return true }
        fun drain() { while (turn()) Unit }
        fun count(e: String) = ran.count { it == e }
        fun bar(tx: Tx, auto: Msg) = SaveStay.bar(tx, mem, rev, auto)
    }

    @Test fun `P2N the first Save on a new capture files it on the press, once, and stays - a kept capture's screen from that answer on`() {
        val p = Phone(filed = false)
        assertEquals(SaveStay.Flow.NEW, SaveStay.flow(p.mem)); assertEquals(SaveStay.Filing.NOT_FILED, SaveStay.filing(p.mem))
        assertTrue(SaveStay.mayDiscard(p.mem), "not filed: Discard this 360 is offered")
        // AT THE PRESS: the read-back save, and the press handed to the keep -- no upload yet (there is no row to upload from)
        assertEquals(listOf(Effect.SAVE, Effect.FILE), SaveStay.effects(6, true, SaveStay.Flow.NEW))
        assertEquals(listOf(Effect.SAVE, Effect.FILE), SaveStay.effects(6, false, SaveStay.Flow.NEW), "a new capture is filed whatever the save answered, as back always filed it")
        assertEquals(listOf(Effect.SAVE), SaveStay.effects(6, null, SaveStay.Flow.NEW)); assertTrue(SaveStay.effects(5, true, SaveStay.Flow.NEW).isEmpty())
        p.press()
        assertEquals(listOf("SAVE"), p.ran)
        assertEquals(SaveStay.Filing.FILING, SaveStay.filing(p.mem)); assertFalse(SaveStay.mayDiscard(p.mem), "being filed: its 360 is being read")
        assertEquals("Survey prep saved — writing its pictures…", p.bar(Tx.EN, auto)); assertTrue(p.open, "it stays")
        // THE KEEP'S TURN: no row, so it files; its answer queues the upload the press held back, once
        assertEquals(listOf(Effect.FILE), SaveStay.keep(filed = false))
        p.turn()
        assertEquals(listOf("SAVE", "FILE", "UPLOAD"), p.ran); assertTrue(p.row)
        assertEquals(SaveStay.Flow.KEPT, SaveStay.flow(p.mem)); assertEquals(SaveStay.Filing.FILED, SaveStay.filing(p.mem))
        assertFalse(SaveStay.mayDiscard(p.mem), "filed: its row points at the 360 file")
        assertFalse(p.mem.held); assertEquals(0, p.mem.keeps)
        assertEquals("Survey prep saved · 13 pictures in the room folder", p.bar(Tx.EN, auto)); assertTrue(p.open)
        // a press whose save did NOT read back still files -- and queues no upload, at the answer or ever for that press
        val q = Phone(filed = false); q.press(readBack = false); q.drain()
        assertEquals(listOf("SAVE", "FILE"), q.ran); assertTrue(q.mem.filed); assertEquals("Not saved: did not read back", q.bar(Tx.EN, auto))
        // a filing that FAILS leaves it not filed, says why, and offers Discard again; the next press files -- once
        val f = Phone(filed = false, fails = 1); f.press(); f.drain()
        assertEquals(listOf("SAVE", "FILE failed"), f.ran); assertFalse(f.mem.filed); assertEquals(SaveStay.Filing.NOT_FILED, SaveStay.filing(f.mem))
        assertTrue(SaveStay.mayDiscard(f.mem)); assertTrue(f.mem.held, "its upload is still held back")
        assertEquals("Survey prep saved on this phone; pictures not written: disk full", f.bar(Tx.EN, auto))
        f.press(); f.drain()
        assertEquals(listOf("SAVE", "FILE failed", "SAVE", "FILE", "UPLOAD"), f.ran); assertTrue(f.mem.filed)
    }

    @Test fun `P2N a second press during the filing files nothing twice - it waits its turn and is a kept capture's press by then`() {
        val p = Phone(filed = false)
        p.press(); p.change(); p.press()
        assertEquals(listOf("SAVE", "SAVE"), p.ran, "both presses save at once; neither files, uploads or writes a picture yet")
        assertEquals(SaveStay.Filing.FILING, SaveStay.filing(p.mem)); assertEquals(2, p.mem.keeps)
        // the first keep's turn: it files. The bar still says "writing" -- the newest press has not been answered
        p.turn()
        assertEquals(listOf("SAVE", "SAVE", "FILE", "UPLOAD"), p.ran); assertTrue(p.mem.filed)
        assertEquals("Survey prep saved — writing its pictures…", p.bar(Tx.EN, auto))
        // the second keep's turn: the row is there, so it keeps the size and rewrites the pictures -- with ITS prep, the
        // one changed after the first press -- and never files
        assertEquals(listOf(Effect.KEEP_SIZE, Effect.PICTURES), SaveStay.keep(filed = true))
        p.turn()
        assertEquals(listOf("SAVE", "SAVE", "FILE", "UPLOAD", "KEEP_SIZE", "PICTURES"), p.ran)
        assertEquals(1, p.count("FILE")); assertEquals(1, p.count("UPLOAD"), "one upload for the two held presses: it carries the prep as last saved")
        assertEquals("Survey prep saved · 6 pictures in the room folder", p.bar(Tx.EN, auto)); assertEquals(0, p.mem.keeps)
        // the first filing FAILS under the second press: the second keep finds no row and files -- once
        val f = Phone(filed = false, fails = 1); f.press(); f.press(); f.drain()
        assertEquals(listOf("SAVE", "SAVE", "FILE failed", "FILE", "UPLOAD"), f.ran); assertEquals(1, f.count("FILE"))
        // three presses in a row: still one filing, and every later one a rewrite
        val t = Phone(filed = false); t.press(); t.press(); t.press(); t.drain()
        assertEquals(1, t.count("FILE")); assertEquals(2, t.count("PICTURES")); assertEquals(1, t.count("UPLOAD"))
        // the last press alone decides the held upload: one that did not read back holds none
        val n = Phone(filed = false); n.press(readBack = true); n.press(readBack = false); n.drain()
        assertEquals(0, n.count("UPLOAD"), "the web never gets a prep this phone does not hold"); assertTrue(n.mem.filed)
    }

    @Test fun `P2N a press after the filing is exactly a kept capture's press - save, upload, size, pictures - and never files`() {
        val p = Phone(filed = false); p.press(); p.drain()
        val kept = Phone(filed = true)
        val before = p.ran.size
        p.change(); p.press(); p.drain()
        kept.change(); kept.press(); kept.drain()
        assertEquals(listOf("SAVE", "UPLOAD", "KEEP_SIZE", "PICTURES"), kept.ran)
        assertEquals(kept.ran, p.ran.drop(before), "the same effects, in the same order, as on a capture opened from its room")
        assertEquals(1, p.count("FILE")); assertEquals(p.bar(Tx.EN, auto), kept.bar(Tx.EN, auto))
        // a save that did not read back there: no upload, the size and the pictures still follow -- the kept rule
        val b = p.ran.size; p.change(); p.press(readBack = false); p.drain()
        assertEquals(listOf("SAVE", "KEEP_SIZE", "PICTURES"), p.ran.drop(b))
        // a capture opened from its room is filed from the start, never files, and was never offered Discard
        assertEquals(SaveStay.OPENED, SaveStay.opened(true)); assertTrue(SaveStay.OPENED.filed); assertFalse(SaveStay.mayDiscard(SaveStay.OPENED))
        assertEquals(0, kept.count("FILE")); assertEquals(SaveStay.Flow.KEPT, SaveStay.flow(SaveStay.OPENED))
    }

    @Test fun `P2N back before any press files the capture as it always has - once, with no upload`() {
        val p = Phone(filed = false)
        assertTrue(SaveStay.leave(p.mem, 0).then.isEmpty(), "no press: nothing was held back")
        assertEquals(listOf(Effect.FILE), SaveStay.keep(filed = false, leaving = true))
        p.back(); p.drain()
        assertEquals(listOf("FILE"), p.ran); assertTrue(p.row); assertFalse(p.open)
        // changes made, no press: the same -- back hands the prep over as it stands, so they are in the filing
        val c = Phone(filed = false); c.change(); c.change(); c.back(); c.drain()
        assertEquals(listOf("FILE"), c.ran)
        // Save pressed with fewer than six surfaces bounded did nothing at all, so back is still the one that files
        val s = Phone(filed = false); s.press(bounded = 5); assertTrue(s.ran.isEmpty()); assertEquals(SaveStay.opened(false), s.mem)
        s.back(); s.drain(); assertEquals(listOf("FILE"), s.ran)
        // back on a capture opened from its room files nothing, ever: it keeps the size
        val k = Phone(filed = true); k.back(); k.drain(); assertEquals(listOf("KEEP_SIZE"), k.ran)
    }

    @Test fun `P2N back after a press files nothing twice and loses nothing - answered, still filing, or failed`() {
        // (a) the press was answered: back keeps the size alone, as on a kept capture
        val a = Phone(filed = false); a.press(); a.drain(); a.back(); a.drain()
        assertEquals(listOf(Effect.KEEP_SIZE), SaveStay.keep(filed = true, leaving = true))
        assertEquals(listOf("SAVE", "FILE", "UPLOAD", "KEEP_SIZE"), a.ran); assertEquals(1, a.count("FILE"))
        // ... and a change made after that Save is said to be behind it (it goes with the next Save, as on a kept capture)
        val a2 = Phone(filed = false); a2.press(); a2.drain(); a2.change()
        assertTrue(SaveStay.leave(a2.mem, a2.rev).changedSince); assertTrue(SaveStay.leave(a2.mem, a2.rev).then.isEmpty())
        // (b) back while the press is STILL FILING: back's keep waits its turn behind it and files nothing; the upload the
        // press held back is queued by back, once -- and the press's late answer, after the screen has closed, queues none
        val b = Phone(filed = false); b.press()
        assertEquals(listOf(Effect.UPLOAD), SaveStay.leave(b.mem, b.rev).then)
        b.back()
        assertEquals(listOf("SAVE", "UPLOAD"), b.ran); assertFalse(b.mem.held)
        b.drain()
        assertEquals(listOf("SAVE", "UPLOAD", "FILE", "KEEP_SIZE"), b.ran); assertEquals(1, b.count("FILE")); assertEquals(1, b.count("UPLOAD"))
        assertTrue(b.mem.filed); assertEquals(0, b.mem.keeps)
        // (c) the press's filing FAILED, then back: back files, as it always has, and the held upload follows -- once each
        val c = Phone(filed = false, fails = 1); c.press(); c.drain(); c.back(); c.drain()
        assertEquals(listOf("SAVE", "FILE failed", "UPLOAD", "FILE"), c.ran)
        // (d) back while the press is filing, and that filing then fails: back's keep finds no row and files -- once
        val d = Phone(filed = false, fails = 1); d.press(); d.back(); d.drain()
        assertEquals(listOf("SAVE", "UPLOAD", "FILE failed", "FILE"), d.ran); assertEquals(1, d.count("FILE")); assertTrue(d.row)
        // (e) the last press did not read back: back queues no upload
        val e = Phone(filed = false); e.press(readBack = false); e.back(); e.drain()
        assertEquals(listOf("SAVE", "FILE", "KEEP_SIZE"), e.ran)
        // back never repeats anything, in any state
        for (m in listOf(a.mem, b.mem, c.mem, d.mem, e.mem, SaveStay.opened(false), SaveStay.OPENED)) assertTrue(SaveStay.leave(m, 0).repeats.isEmpty())
    }

    @Test fun `P2N every order of presses, changes, turns and back - one filing at most, nothing before its row, nothing lost`() {
        // EVERY sequence of up to seven events on a new capture -- a press that reads back (P), one that does not (N), a
        // change (C), the keep taking one turn (T), back (B, after which only turns) -- with the first 0, 1 or 2 filings failing
        val events = "PNCTB"
        var walked = 0
        fun walk(seq: String) {
            for (fails in 0..2) {
                val p = Phone(filed = false, fails = fails)
                var lastReadBack: Int? = null   // where in `ran` the last press that read back saved
                for (e in seq) {
                    when (e) {
                        'P' -> if (p.open) { p.press(true); lastReadBack = p.ran.size - 1 - (if (p.ran.last() == "UPLOAD") 1 else 0) }
                        'N' -> if (p.open) { p.press(false); lastReadBack = null }
                        'C' -> if (p.open) p.change()
                        'T' -> p.turn()
                        'B' -> if (p.open) p.back()
                    }
                    // the memory's own rules, after every event
                    assertTrue(p.mem.keeps >= 0, seq); assertFalse(p.mem.filed && p.mem.held, "$seq: filed, nothing is held")
                    assertEquals(p.mem.filed, SaveStay.flow(p.mem) == SaveStay.Flow.KEPT)
                    if (p.mem.filed) assertTrue(p.row, "$seq: the screen says filed only of a capture that has its row")
                    assertEquals(!p.mem.filed && p.mem.keeps == 0, SaveStay.mayDiscard(p.mem), seq)
                }
                p.drain()
                walked++
                val tries = p.count("FILE") + p.count("FILE failed")
                assertTrue(p.count("FILE") <= 1, "$seq/$fails: filed twice: ${p.ran}")
                if (tries > fails) assertEquals(1, p.count("FILE"), "$seq/$fails: ${p.ran}") else assertEquals(0, p.count("FILE"))
                if (p.count("FILE") == 1) {
                    assertEquals(fails + 1, tries, "$seq/$fails: nothing files after the filing: ${p.ran}")
                    val at = p.ran.indexOf("FILE")
                    assertTrue(p.ran.drop(at + 1).none { it.startsWith("FILE") })
                    // a picture rewrite and a kept size only ever follow the filing
                    assertTrue(p.ran.take(at).none { it == "PICTURES" || it == "KEEP_SIZE" }, "$seq/$fails: ${p.ran}")
                } else assertTrue(p.ran.none { it == "PICTURES" || it == "KEEP_SIZE" })
                assertEquals(0, p.mem.keeps, "$seq/$fails: every press was answered")
                // back with a filing still to try always tries it: a capture is left unfiled only when every try failed
                if (!p.open && !p.row) assertEquals(tries, p.count("FILE failed"))
                if ('B' in seq && fails == 0) assertTrue(p.row, "$seq: back always files")
                // NOTHING LOST: the last press that read back is uploaded -- at it or after it -- once the capture is filed
                val last = lastReadBack
                if (last != null && p.row) assertTrue(p.ran.drop(last + 1).contains("UPLOAD"), "$seq/$fails: ${p.ran}")
                // ... and never more uploads than presses that read back
                assertTrue(p.count("UPLOAD") <= seq.takeWhile { it != 'B' }.count { it == 'P' } , "$seq/$fails: ${p.ran}")
                // the bar never waits for ever: with nothing changed since a press that read back, it is not "writing" once every keep has answered
                if (p.mem.at == p.rev && p.mem.readBack) assertFalse(p.bar(Tx.EN, auto) == Tx.EN.app_msg_survey_prep_saved_writing, "$seq/$fails")
            }
        }
        fun all(prefix: String, n: Int) { walk(prefix); if (n > 0) for (e in events) all(prefix + e, n - 1) }
        all("", 7)
        assertEquals(3 * (0..7).sumOf { Math.pow(5.0, it.toDouble()).toInt() }, walked)
    }

    @Test fun `P2N the screen's memory - an older answer shows nothing but still files, and a failed rewrite never un-files`() {
        // every press that may act hands something to the keep, in both flows and whatever the save answered: saved() counts on it
        for (flow in SaveStay.Flow.values()) for (rb in listOf(true, false)) assertTrue(SaveStay.toKeep(SaveStay.effects(6, rb, flow)), "$flow $rb")
        assertFalse(SaveStay.toKeep(SaveStay.effects(6, null))); assertFalse(SaveStay.toKeep(SaveStay.effects(5, true)))
        // FILE is the keep's alone, and only with no row: no press, no back and no order of them can ask for a second
        for (leaving in listOf(true, false)) {
            assertEquals(listOf(Effect.FILE), SaveStay.keep(filed = false, leaving = leaving))
            assertFalse(Effect.FILE in SaveStay.keep(filed = true, leaving = leaving))
        }
        assertFalse(Effect.FILE in SaveStay.effects(6, true, SaveStay.Flow.KEPT)); assertFalse(Effect.PICTURES in SaveStay.keep(true, leaving = true))
        // an OLDER press's answer changes nothing the bar shows -- and still says the capture is filed
        var m = SaveStay.saved(SaveStay.saved(SaveStay.opened(false), 1, true), 2, true)
        val a = SaveStay.answered(m, 1, 13, null)
        assertTrue(a.mem.filed); assertEquals(SaveStay.Pictures.WRITING, a.mem.pictures); assertEquals(listOf(Effect.UPLOAD), a.then); assertEquals(1, a.mem.keeps)
        // the newest press's rewrite then FAILS: said on the bar, and the capture stays filed -- nothing is held, nothing queued twice
        val b = SaveStay.answered(a.mem, 2, null, "disk full")
        assertTrue(b.mem.filed); assertEquals(SaveStay.Pictures.NOT_WRITTEN, b.mem.pictures); assertEquals("disk full", b.mem.why); assertTrue(b.then.isEmpty())
        assertEquals(0, b.mem.keeps); assertEquals(0, SaveStay.answered(b.mem, 2, 6, null).mem.keeps, "never below none")
        // on a kept capture an answer is what written / notWritten always were, and queues nothing
        m = SaveStay.saved(SaveStay.OPENED, 4, true)
        assertFalse(m.held)
        SaveStay.answered(m, m.seq, 6, null).let { assertEquals(SaveStay.written(m, m.seq, 6).copy(keeps = 0), it.mem); assertTrue(it.then.isEmpty()) }
        SaveStay.answered(m, m.seq, null, "x").let { assertEquals(SaveStay.notWritten(m, m.seq, "x").copy(keeps = 0), it.mem); assertTrue(it.then.isEmpty()) }
        // left() takes the held upload with it, and nothing else
        val h = SaveStay.saved(SaveStay.opened(false), 1, true)
        assertTrue(h.held); assertEquals(h.copy(held = false), SaveStay.left(h)); assertEquals(SaveStay.OPENED, SaveStay.left(SaveStay.OPENED))
        assertTrue(SaveStay.answered(SaveStay.left(h), 1, 13, null).then.isEmpty(), "the late answer queues no second upload")
        // no key says "filed when you go back" any more: the press files
        assertEquals(listOf("NONE", "WRITING", "WRITTEN", "NOT_WRITTEN"), SaveStay.Pictures.values().map { it.name })
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
