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
 *  - A NEW CAPTURE ([Flow.NEW]), which ← and ✓ Save both FILE today: a press saves alone, and the keep on ← files it and
 *    then queues the upload the press held back, once ([effects], [Leave.then]).
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
    }

    /**
     * WHICH SURVEY PREP THIS IS: a KEPT capture's, opened from its room (`MainActivity`'s `surveyKept`), or a NEW
     * capture's, straight after the shutter (`MainActivity`'s `keepWith`, which files the capture -- its six faces and
     * its row, `FaceWriter.commit` and `store.insert`, once -- on ✓ Save and on ← alike).
     */
    enum class Flow { KEPT, NEW }

    /**
     * WHAT ONE PRESS OF ✓ SAVE DOES, IN ORDER, EACH ONCE: nothing at all while fewer than six surfaces are bounded
     * ([may]); else [Effect.SAVE], then -- [readBack]: what it answered -- [Effect.UPLOAD] only when it read back, then
     * [Effect.KEEP_SIZE] and [Effect.PICTURES] whatever it answered, as today's save callback runs them. NOT among them:
     * leaving. [readBack] null: the save has not answered yet -- only [Effect.SAVE].
     *
     * A NEW CAPTURE ([Flow.NEW]) IS NOT FILED UNTIL IT IS KEPT, and keeping it is what leaving does ([leave]): so a press
     * there is the read-back save ALONE. Its upload waits for the keep, because the upload worker refuses a capture
     * whose row does not exist after five tries (`AsIsUpload`: "waiting to be filed", then "capture gone") -- and
     * staying is exactly what would outlast them; its room size and its pictures are the keep's own (`keepWith` hands
     * the size over, `FaceWriter.commit` writes the faces with their survey marks).
     */
    fun effects(bounded: Int, readBack: Boolean?, flow: Flow = Flow.KEPT): List<Effect> = when {
        !may(bounded) -> emptyList()
        readBack == null || flow == Flow.NEW -> listOf(Effect.SAVE)
        else -> listOfNotNull(Effect.SAVE, Effect.UPLOAD.takeIf { readBack }, Effect.KEEP_SIZE, Effect.PICTURES)
    }

    /** Where the last press's pictures are. */
    enum class Pictures { NONE, WRITING, WRITTEN, NOT_WRITTEN }

    /**
     * WHAT SURVEY PREP REMEMBERS OF ITS LAST SAVE: the change count it saved ([at]: the screen's `dirty` then; null --
     * not saved since it opened), whether it [readBack], where its [pictures] are and how many were [written] (or
     * [why] not), and the press it was ([seq]).
     */
    data class Mem(val at: Int? = null, val readBack: Boolean = false, val pictures: Pictures = Pictures.NONE,
                   val written: Int = 0, val why: String? = null, val seq: Int = 0)

    /** Survey prep just opened: no save yet. */
    val OPENED = Mem()

    /** A press saved the change count [rev], and the save [readBack] or did not; its pictures are being written --
     *  unless [pictures] is false: a NEW capture's press writes none ([effects], [Flow.NEW]), its pictures being the
     *  keep's own. The press is the next [Mem.seq]. */
    fun saved(m: Mem, rev: Int, readBack: Boolean, pictures: Boolean = true): Mem =
        Mem(rev, readBack, if (pictures) Pictures.WRITING else Pictures.NONE, 0, null, m.seq + 1)

    /** Press [seq]'s pictures were written, [n] of them. An older press's answer changes nothing. */
    fun written(m: Mem, seq: Int, n: Int): Mem = if (seq != m.seq) m else m.copy(pictures = Pictures.WRITTEN, written = n, why = null)

    /** Press [seq]'s pictures were not written, for the reason [why] (the system's own words). An older press's
     *  answer changes nothing. */
    fun notWritten(m: Mem, seq: Int, why: String): Mem = if (seq != m.seq) m else m.copy(pictures = Pictures.NOT_WRITTEN, why = why)

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
            // a press that writes no pictures (a NEW capture's, [saved]): the save's own line is the autosave's -- "Saved
            // ✓ 14:05" -- never "writing its pictures…", which would wait for ever
            Pictures.NONE -> auto(tx)
            Pictures.WRITING -> tx.app_msg_survey_prep_saved_writing
            Pictures.WRITTEN -> tx.app_msg_survey_prep_saved_x(m.written.toString())
            Pictures.NOT_WRITTEN -> tx.app_msg_survey_prep_saved_not_written_x(m.why ?: "")
        }
    }

    /**
     * What ← does beyond what it always has: [repeats] -- never anything: no Effect a press ran is run again by leaving;
     * [then] -- for a NEW capture whose last Save read back, the upload that press did not queue, ONCE, after the keep
     * has filed its row ([effects]); nothing else, ever; [changedSince] -- a change was made after the last Save (kept
     * on the phone; on a kept capture it goes to the web and the pictures with the next Save).
     */
    class Leave(val repeats: List<Effect>, val then: List<Effect>, val changedSince: Boolean)

    /** ← after [m], with the change count [rev] now, in [flow]. */
    fun leave(m: Mem, rev: Int, flow: Flow = Flow.KEPT): Leave =
        Leave(emptyList(), if (flow == Flow.NEW && m.at != null && m.readBack) listOf(Effect.UPLOAD) else emptyList(), changedSince(m, rev))
}
