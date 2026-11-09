package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.Album.Op
import java.time.Instant
import kotlin.random.Random

/**
 * THE PROJECT MANAGER'S TAPS AS OPS (port 61c slice K4, 9 Nov 2026; DESIGN-port61c.md §19), pure. Each tap on the album
 * is checked by K1b's gate ([Album.stateWrite], [Album.crRaise], [Album.crAnswer], [Album.review]) on the OVERLAID state
 * -- what the screen shows, this login's queue laid over the store and the mirror ([AlbumPage.model]) -- and built by
 * K1b's op builder on the MIRROR's bases ([Album.stateOp], [Album.crRaiseOp], [Album.crAnswerOp], [Album.reviewOp]),
 * so a chain of taps offline is exactly the chain S5 will take (AQ rebases each link on the answer before it). The op
 * carries the tap's instant ([Op.at]), which the overlay draws and the wire never sends.
 *
 * The prototype refuses most writes by drawing no control (its writers return null and draw nothing, index61c2.html
 * 7220, 7279, 7289): such a tap is [Out.None]. Only two refusals have words on the prototype's sheets: leaving Done with
 * no reason (the state sheet's need line, 7546) and a client's answer whose date is not a day (S5's 400, verbatim,
 * 7535) -- [Out.Refused]. Nobody but the Project Manager ([AlbumPage.canWrite]) gets anything but [Out.None].
 */
object AlbumWrite {

    /** What a tap does: queue [Queue.op]; stay open with [Refused.why]'s words; or nothing ([None], not Kotlin's `Nothing`). */
    sealed interface Out {
        data class Queue(val op: Op) : Out
        data class Refused(val why: Album.Refusal) : Out
        data object None : Out
    }

    /**
     * A state picked on the state sheet: `from` is the OVERLAID state, sent as `prev` (S5's SQL compares it with its own);
     * the current state is [Out.None] (the prototype's pick of the current state closes the sheet, 7544); leaving Done
     * with a blank [remark] is the need line; any other refusal is [Out.None] (the chip is not a tap then).
     */
    fun state(m: AlbumPage.Model, work: Int, to: String, remark: String, now: Instant, random: Random = Random.Default): Out {
        val w = m.write ?: return Out.None
        val from = Album.stateName(w.states[work]?.state)
        return when (val r = Album.stateWrite(w.viewer, w.exec, work, from, to, remark)) {
            null -> Out.Queue(Album.stateOp(w.mirror, work, from, to, remark, random).copy(at = now.toString()))
            Album.Refusal.DONE_REASON_NEED -> Out.Refused(r)
            else -> Out.None
        }
    }

    /** The kinds a change request on [work] may be (index61c2.html 7521): in the OVERLAID approved scope remove and change, outside it add. */
    fun kinds(m: AlbumPage.Model, work: Int): List<Album.CrKind> {
        val scope = m.write?.scope
        return if (scope != null && scope.contains(work)) listOf(Album.CrKind.REMOVE, Album.CrKind.CHANGE) else listOf(Album.CrKind.ADD)
    }

    /**
     * A change request raised from the raise sheet, with its [kind] and the [note] the client reads (blank allowed, 7526).
     * Refused is [Out.None] and the sheet closes, as the prototype's raiseCR returning null closes it (`m.sheet=null`
     * whatever it returned) -- reachable only through a race, a read landing between the sheet's open and its button.
     */
    fun raise(m: AlbumPage.Model, work: Int, kind: Album.CrKind, note: String, now: Instant, random: Random = Random.Default): Out {
        val w = m.write ?: return Out.None
        if (Album.crRaise(w.viewer, w.stage, w.scope, work, kind, w.log) != null) return Out.None
        return Out.Queue(Album.crRaiseOp(w.mirror, work, kind, note, random).copy(at = now.toString()))
    }

    /**
     * The client's answer recorded from the answer sheet: [Album.crAnswer]'s gate; a date that is not a day for an
     * approved or declined answer is [Out.Refused] (the sheet's status line, S5's 400 verbatim), every other refusal
     * [Out.None] (the K4 critic, finding 6: the sheet closes). The op's `prev`, the kind it answers, comes from the
     * OVERLAID log, so an answer to a raise still queued carries its kind.
     */
    fun answer(
        m: AlbumPage.Model, work: Int, outcome: Album.CrOutcome, clientAt: String, how: String, now: Instant,
        random: Random = Random.Default,
    ): Out {
        val w = m.write ?: return Out.None
        return when (val r = Album.crAnswer(w.viewer, w.stage, work, outcome, clientAt, w.log)) {
            null -> Out.Queue(Album.crAnswerOp(w.mirror, w.log, work, outcome, clientAt, how, random).copy(at = now.toString()))
            Album.Refusal.CLIENT_AT_DAY -> Out.Refused(r)
            else -> Out.None
        }
    }

    /**
     * Accept / Not for client on the item [key]: [Album.review] on the OVERLAID photo (so a second Accept is REVIEW_SAME,
     * nothing queued), the op built from the MIRROR's photo -- `base` is S5's `review_at`, never the overlay's tap time,
     * which S5 would answer as stale (asis.js guards the review on the OLD review_at); a second review of the same photo
     * queued behind the first takes the first's answer's `review_at` through AQ's rebase.
     */
    fun review(m: AlbumPage.Model, key: String, state: String, now: Instant, random: Random = Random.Default): Out {
        val w = m.write ?: return Out.None
        val p = w.items[key] ?: return Out.None
        if (p.source != Album.Source.ALBUM) return Out.None
        val held = w.mirror.photos.firstOrNull { it.id == p.id } ?: return Out.None
        if (Album.review(w.viewer, p, state) != null) return Out.None
        return Out.Queue(Album.reviewOp(held, state, random).copy(at = now.toString()))
    }

    /** The IST day of [now], YYYY-MM-DD (the prototype's C.today, 7332) -- through atZone, never LocalDate.ofInstant, which is Android API 34. */
    fun today(now: Instant): String = now.atZone(QuoteStage.IST).toLocalDate().toString()

    /**
     * [op] AS THE OUTBOX STORES IT (the K4 critic, finding 1), called by :app's AlbumOutbox.add under the store's lock:
     * [seen] is the queue the screen drew when the tap was built, [stored] the queue as stored now, [mirror] the mirror
     * FILE. The drain commits S5's answer to an op onto the phone (the mirror, the store) BEFORE it removes the op, so an
     * op the screen still saw queued and the stored queue no longer holds was answered, and the file holds S5's row for
     * it; a tap built in that gap took its base from a screen that had not caught up. Such an op takes, as AQ's rebase
     * would have given it had it been queued a moment sooner, the seq of the newest row S5 took for those predecessors on
     * its work (a log op), or the `at` of the newest review S5 took for its photo (a review: S5's `review_at`). Taken
     * from THOSE rows alone, never from the file's newest -- a row another writer put there would make the op overwrite
     * a change the person never saw, which S5's guard exists to refuse. Every other op is [op] as it came.
     */
    fun atAdd(op: Op, seen: List<Op>, stored: List<Op>, mirror: Album.View?): Op {
        if (mirror == null) return op
        val left = stored.map { it.id }.toSet()
        val answered = seen.filter { it.id !in left }.map { it.id }.toSet()
        if (answered.isEmpty()) return op
        val rows = mirror.log.filter { it.id in answered }
        return when (op.kind) {
            Album.KIND_STATE, Album.KIND_CR_RAISE, Album.KIND_CR_CLEAR -> {
                val seq = rows.filter { it.work == op.work && it.kind != Album.KIND_STAGE }.maxOfOrNull { it.seq } ?: return op
                if (seq > (op.base.trim().toLongOrNull() ?: 0L)) op.copy(base = seq.toString()) else op
            }
            Album.OP_REVIEW -> {
                val e = rows.filter { it.kind == Album.KIND_REVIEW && it.photo == op.photo }.maxByOrNull { it.seq } ?: return op
                if (e.at.isBlank()) op else op.copy(base = e.at)
            }
            else -> op
        }
    }
}
