package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Msg
import com.malletcrafts.interiorestimate.pano.i18n.Tx

/**
 * ✓ SAVE SAVES AND STAYS (port 63 slice I1c, 23 Nov 2026; DESIGN-imagemeter §21, P2). Amit, after using 0.3.345: "Click
 * on save always take me out to room foto. its annoying." So ✓ Save does EVERYTHING a save does today -- the read-back
 * save, the as-is upload queued when it read back, the room size kept on the capture's row, the room folder's survey
 * pictures rewritten and the result said -- and survey prep STAYS OPEN on the same face, step and zoom, nothing picked
 * or gold changed (a save touches no [Pick.State]); only ← leaves. When Save may act is unchanged in this slice: all six
 * surfaces bounded ([may]).
 *
 * What the screen then needs that it did not, held here because the screen is compiled only by CI:
 *  - WHAT HAPPENS ON A PRESS, EACH EXACTLY ONCE ([effects]): the save first, then -- by its answer -- the upload, then
 *    the room size and the pictures (`MainActivity`'s save callback, which until now also closed the dialog);
 *  - WHAT THE BAR'S SAVED STATE READS AFTER IT ([bar]): the save's own words -- "Survey prep saved — writing its
 *    pictures…", then "Survey prep saved · 6 pictures in the room folder" or the reason they were not written, or
 *    "Not saved: did not read back" -- for as long as nothing has changed since; from the next change on, the
 *    autosave's own line, so the bar never says "pictures written" of a room that has changed since they were;
 *  - WHICH ANSWER IS SHOWN when two presses overlap ([Mem.seq]): only the newest press's -- an older rewrite that ends
 *    late does not write its count over the newer one's "writing…";
 *  - WHAT ← DOES AFTER A SAVE ([leave]): what it always has, and nothing of the save again -- no second upload, no second
 *    rewrite. Every change is already on disk (the autosave, and the save on leaving), so nothing is lost; a change
 *    made AFTER the last Save is kept on the phone and goes to the web and the pictures with the next Save, exactly as
 *    a change ← leaves with always has ([Leave.changedSince] says whether there is one);
 *  - A NEW CAPTURE ([Flow.NEW]), straight after the shutter, which has no row and no pictures until it is FILED
 *    ([Effect.FILE]). Slice I1 left its filing on ← (the review of I1, finding 2: "✓ Save still files the capture only
 *    when he presses ←"); since port 63 P2N the FIRST ✓ SAVE FILES IT, ON THE PRESS, EXACTLY ONCE, AND STAYS -- and from
 *    that answer on the open screen is a kept capture's ([flow]), so later presses save, queue the upload and rewrite the
 *    pictures exactly as on an opened capture. Three places run it, and each asks here: the screen at the press
 *    ([effects]), the caller's keep, ONE AT A TIME, for a press and for ← alike ([keep] -- the one place that decides
 *    whether to file, by whether the row exists THEN), and the screen again when that keep answers ([answered]).
 */
object SaveStay {

    /** Save acts only with all six surfaces bounded -- unchanged in this slice. */
    fun may(bounded: Int): Boolean = bounded == SurveyPrep.SURFACES.size

    /** One effect of a press. */
    enum class Effect {
        /** `store.save`, then its read-back: the one that decides the next. */
        SAVE,
        /** `AsIsUpload.enqueue`, and the web state read again -- only after a save that read back. */
        UPLOAD,
        /** The room size as survey prep has it, to the capture's row (`keepRoomSize`). */
        KEEP_SIZE,
        /** The room folder's survey pictures, rewritten from the lines as they now stand (`FaceWriter.rewriteSurvey`). */
        PICTURES,
        /** A NEW capture FILED (port 63 P2N): its six faces and its survey pictures into the room folder
         *  (`FaceWriter.commit`) and its row (`store.insert`), at the size and with the prep handed over. ONCE IN THE
         *  CAPTURE'S LIFE: a second one would write the six faces again and insert a second row ([keep]). */
        FILE,
    }

    /**
     * WHICH SURVEY PREP THIS IS, NOW: a KEPT capture's -- one that has its row and its pictures: opened from its room
     * (`MainActivity`'s `surveyKept`), or a new one once its first ✓ Save has filed it -- or a NEW capture's, straight
     * after the shutter and not filed yet (`MainActivity`'s `NewCaptureSurvey`). The screen asks [flow] at every press:
     * it changes under an open screen, once, from NEW to KEPT.
     */
    enum class Flow { KEPT, NEW }

    /**
     * WHAT ONE PRESS OF ✓ SAVE DOES ON THE SCREEN, AT THE PRESS, IN ORDER, EACH ONCE: nothing at all while fewer than six
     * surfaces are bounded ([may]); else [Effect.SAVE], then -- [readBack]: what it answered; null: not yet, only
     * [Effect.SAVE] -- by [flow]:
     *  - KEPT: [Effect.UPLOAD] only when it read back, then [Effect.KEEP_SIZE] and [Effect.PICTURES] whatever it
     *    answered, as the save callback always ran them. The last two are the caller's ([keep]).
     *  - NEW: [Effect.FILE], whatever the save answered (← has always filed a capture whose prep did not read back, and
     *    so does the press) -- which here means: THIS PRESS IS HANDED TO THE CALLER'S KEEP, with the prep and the size
     *    as they stand, and the keep decides under its lock what is still to do ([keep]): file, or -- when a press
     *    before it has filed the capture meanwhile -- keep the size and rewrite the pictures. NO UPLOAD AT THE PRESS:
     *    the upload worker refuses a capture whose row does not exist after five tries (`AsIsUpload`: "waiting to be
     *    filed", then "capture gone"), so the upload waits for the keep's answer ([answered]).
     * NOT among them, ever: leaving.
     */
    fun effects(bounded: Int, readBack: Boolean?, flow: Flow = Flow.KEPT): List<Effect> = when {
        !may(bounded) -> emptyList()
        readBack == null -> listOf(Effect.SAVE)
        flow == Flow.NEW -> listOf(Effect.SAVE, Effect.FILE)
        else -> listOfNotNull(Effect.SAVE, Effect.UPLOAD.takeIf { readBack }, Effect.KEEP_SIZE, Effect.PICTURES)
    }

    /** Whether a press with these [effects] hands anything to the caller's keep -- every press that [may] act does,
     *  in both flows; [saved] counts on it ([Mem.keeps]). */
    fun toKeep(fx: List<Effect>): Boolean = Effect.FILE in fx || Effect.KEEP_SIZE in fx || Effect.PICTURES in fx

    /**
     * WHAT THE CALLER'S KEEP DOES WITH ONE PRESS, OR WITH ←, WHEN ITS TURN COMES (port 63 P2N). The keep runs ONE AT A
     * TIME, in the order the presses were made (`MainActivity`: a `Mutex`, taken from the main thread), and [filed] is
     * read THEN, under that lock -- whether the capture's row exists -- never at the press. So:
     *  - not filed: [Effect.FILE], for a press and for ← alike (← before any press files as it always has). It files at
     *    the size and with the prep of THE CALL IT IS, so its pictures and its row's size are that call's own;
     *  - filed, a press: [Effect.KEEP_SIZE], then [Effect.PICTURES] -- a kept capture's press, which is what a press
     *    made while an earlier one was still filing has become by the time its turn comes;
     *  - filed, ←: [Effect.KEEP_SIZE] alone, as ← on a kept capture keeps the size and writes no picture.
     * THIS IS THE ONE PLACE THAT DECIDES TO FILE, and it can answer [Effect.FILE] only while there is no row: two presses,
     * a press and a ←, or a ← while a press is still filing can never file twice. A filing that FAILED left no row, so
     * the next call files -- a failed filing is retried, never repeated.
     */
    fun keep(filed: Boolean, leaving: Boolean = false): List<Effect> = when {
        !filed -> listOf(Effect.FILE)
        leaving -> listOf(Effect.KEEP_SIZE)
        else -> listOf(Effect.KEEP_SIZE, Effect.PICTURES)
    }

    /** Where the last press's pictures are. (A NEW capture's first press WRITES them, by filing it: slice I1's
     *  `ON_LEAVE` -- "the capture is filed when you go back" -- went with port 63 P2N.) */
    enum class Pictures { NONE, WRITING, WRITTEN, NOT_WRITTEN }

    /**
     * WHAT SURVEY PREP REMEMBERS OF ITS LAST SAVE: the change count it saved ([at]: the screen's `dirty` then; null --
     * not saved since it opened), whether it [readBack], where its [pictures] are and how many were [written] (or
     * [why] not), and the press it was ([seq]).
     *
     * AND OF THE CAPTURE'S FILING (port 63 P2N): whether it is [filed] -- it has its row and its pictures: always, for a
     * capture opened from its room; for a new one, from the first keep that answers with pictures ([answered]) -- how
     * many presses have been handed to the caller's keep and not answered yet ([keeps]), and whether an upload is
     * [held] back: the last press's save read back and the capture had no row to upload from. Filed, nothing is ever
     * held: [answered] queues it at the answer that files.
     */
    data class Mem(val at: Int? = null, val readBack: Boolean = false, val pictures: Pictures = Pictures.NONE,
                   val written: Int = 0, val why: String? = null, val seq: Int = 0,
                   val filed: Boolean = true, val keeps: Int = 0, val held: Boolean = false)

    /** Survey prep just opened on a KEPT capture: no save yet. */
    val OPENED = Mem()

    /** Survey prep just opened, on a capture that is [filed] (opened from its room) or is not (a new one, straight
     *  after the shutter): no save yet. */
    fun opened(filed: Boolean): Mem = if (filed) OPENED else Mem(filed = false)

    /** Which survey prep this is NOW ([Flow]): a kept capture's once it is filed. */
    fun flow(m: Mem): Flow = if (m.filed) Flow.KEPT else Flow.NEW

    /** Where a capture's filing stands, as the screen knows it. */
    enum class Filing { NOT_FILED, FILING, FILED }

    /** [Filing.FILING]: not filed, and a press is with the caller's keep -- its answer will say. */
    fun filing(m: Mem): Filing = if (m.filed) Filing.FILED else if (m.keeps > 0) Filing.FILING else Filing.NOT_FILED

    /**
     * "Discard this 360" drops a new capture's 360 file -- so it is offered only while the capture is NOT FILED and no
     * press is filing it ([Filing.NOT_FILED]): a filed capture's row points at that file, and one being filed is being
     * read from it. A kept capture never had it. (After a filing that failed it is offered again: nothing was filed.)
     */
    fun mayDiscard(m: Mem): Boolean = filing(m) == Filing.NOT_FILED

    /** A press saved the change count [rev], and the save [readBack] or did not; it is handed to the caller's keep
     *  ([effects], [toKeep]: one more of [Mem.keeps]) and its pictures are being written -- by the filing, on a new
     *  capture. On a capture not filed yet the upload is held back by a save that read back, and by the LAST press
     *  alone: the web never gets a prep this phone does not hold. The press is the next [Mem.seq]. */
    fun saved(m: Mem, rev: Int, readBack: Boolean): Mem =
        m.copy(at = rev, readBack = readBack, pictures = Pictures.WRITING, written = 0, why = null, seq = m.seq + 1,
            keeps = m.keeps + 1, held = !m.filed && readBack)

    /** Press [seq]'s pictures were written, [n] of them. An older press's answer changes nothing. */
    fun written(m: Mem, seq: Int, n: Int): Mem = if (seq != m.seq) m else m.copy(pictures = Pictures.WRITTEN, written = n, why = null)

    /** Press [seq]'s pictures were not written, for the reason [why] (the system's own words). An older press's
     *  answer changes nothing. */
    fun notWritten(m: Mem, seq: Int, why: String): Mem = if (seq != m.seq) m else m.copy(pictures = Pictures.NOT_WRITTEN, why = why)

    /** What the screen does when the caller's keep answers a press: remember [mem], and run [then] -- the upload a
     *  press held back, ONCE, at the answer that says the capture is filed; nothing else, ever. */
    class Answer(val mem: Mem, val then: List<Effect>)

    /**
     * THE CALLER'S KEEP ANSWERED PRESS [seq] (port 63 P2N): [n] pictures written, or null and [why] not. Answers come in
     * the order the presses were made (the keep runs one at a time, and answers before it lets the next in).
     *  - WHAT THE BAR SHOWS is the newest press's answer alone ([written], [notWritten]): an older one changes nothing
     *    of it.
     *  - WHETHER THE CAPTURE IS FILED is taken from EVERY answer, the newest press's or not: pictures were written, so
     *    its row exists -- a filing writes them, and a rewrite reads its row. An answer with none never un-files it: a
     *    rewrite that failed is of a capture some earlier answer filed; a FILING that failed leaves it as it was, not
     *    filed, and the next press or ← files ([keep]).
     *  - THE UPLOAD HELD BACK is queued at the answer that files, once ([Answer.then]) -- however many presses were
     *    held -- and from then on every press queues its own, at the press ([effects], [Flow.KEPT]).
     * One of [Mem.keeps] is answered.
     */
    fun answered(m: Mem, seq: Int, n: Int?, why: String?): Answer {
        val filed = m.filed || n != null
        val shown = if (n != null) written(m, seq, n) else notWritten(m, seq, why.orEmpty())
        return Answer(shown.copy(filed = filed, keeps = maxOf(0, m.keeps - 1), held = m.held && !filed),
            if (filed && m.held) listOf(Effect.UPLOAD) else emptyList())
    }

    /** Whether anything has changed since the last Save ([rev]: the screen's change count now). Never saved: false --
     *  there is no save for it to have changed since. */
    fun changedSince(m: Mem, rev: Int): Boolean = m.at != null && m.at != rev

    /**
     * WHAT THE BAR'S SAVED STATE READS ([rev]: the change count now; [auto]: the autosave's own line, as the bar shows
     * it today). After a Save and until the next change: the save's own words -- "Not saved: did not read back" when it
     * did not, else the pictures' state -- and from the next change on, [auto]. Never saved: [auto].
     */
    fun bar(tx: Tx, m: Mem, rev: Int, auto: Msg): String {
        if (m.at == null || m.at != rev) return auto(tx)
        if (!m.readBack) return tx.sp_msg_not_saved_did_not
        return when (m.pictures) {
            Pictures.NONE -> auto(tx)
            Pictures.WRITING -> tx.app_msg_survey_prep_saved_writing
            Pictures.WRITTEN -> tx.app_msg_survey_prep_saved_x(m.written.toString())
            Pictures.NOT_WRITTEN -> tx.app_msg_survey_prep_saved_not_written_x(m.why ?: "")
        }
    }

    /**
     * What ← does beyond what it always has: [repeats] -- never anything: no Effect a press ran is run again by leaving;
     * [then] -- the upload a press on a capture not filed yet held back and no answer has queued since ([Mem.held]),
     * ONCE, after ←'s own keep has been asked: the capture is filed by then or within the moment (by ←'s keep when the
     * press's filing failed, by the press's own when it is still running), and the upload worker waits for its row;
     * nothing else, ever; [changedSince] -- a change was made after the last Save: it is kept on the phone, and goes to
     * the web and the pictures with the next Save, on a capture a press has filed as on one opened from its room. (A
     * new capture that ← itself files is filed with the prep as it stands at ←, so nothing is behind there.)
     */
    class Leave(val repeats: List<Effect>, val then: List<Effect>, val changedSince: Boolean)

    /**
     * ← after [m], with the change count [rev] now. What ←'S OWN KEEP does is [keep]'s answer, `keep(filed, leaving =
     * true)`: it files a capture no press has filed -- as ← always has on a new capture -- and never one that is; a ←
     * pressed while a press is still filing waits its turn behind it and then keeps the size alone.
     */
    fun leave(m: Mem, rev: Int): Leave =
        Leave(emptyList(), if (m.held) listOf(Effect.UPLOAD) else emptyList(), changedSince(m, rev))

    /** ← has taken what [leave] gave it: nothing is held any more, so an answer that arrives after survey prep has
     *  closed (a press still filing when he left) queues no second upload ([answered]). */
    fun left(m: Mem): Mem = m.copy(held = false)
}
