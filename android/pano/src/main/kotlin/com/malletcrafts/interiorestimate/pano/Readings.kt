package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.math.abs

/**
 * SITE READINGS, TWO WRITERS (Amit, 2026-10-11, "Readings on the web"): the
 * phone's survey prep and the /projects page on MCFT Interior Estimate both
 * set readings, and the web's per-key table is the one truth. This is the
 * phone's half of the merge, pure so every case is tested here.
 *
 * The phone keeps, beside its local `actual`, what it last SAW synced for each
 * key: the value and the server's `at` ([Seen]). A key whose local value differs
 * from what was seen is an UNSENT LOCAL EDIT (a delete included).
 *
 *  - [push]: the unsent edits, each with the `at` it was based on, so the
 *    server refuses one somebody else changed since (a conflict) instead of
 *    overwriting it.
 *  - [pull]: a server key changed since it was seen is taken, deletes included
 *    -- unless the phone holds an unsent edit to it, which stays and is pushed.
 *  - [afterPush]: written keys become seen; a conflict means the server value
 *    wins locally, and the screen says who changed it.
 */
object Readings {

    /** What the phone last saw synced for one key: value (null = absent) and the server's `at` (null = never on the server). */
    class Seen(val v: Double?, val at: String?)

    /** One server reading. */
    class Remote(val v: Double?, val by: String, val at: String)

    /** A PATCH body: keys to set (null deletes) and the `at` each was based on. */
    class Push(val set: Map<String, Double?>, val base: Map<String, String?>) {
        fun isEmpty() = set.isEmpty()
    }

    /** The local readings and baseline after a merge, and what changed for the screen to say. */
    class Merge(val actual: Map<String, Double>, val seen: Map<String, Seen>,
                val taken: List<String>, val conflicts: Map<String, Remote>)

    fun same(a: Double?, b: Double?): Boolean =
        if (a == null || b == null) a == null && b == null else abs(a - b) < 1e-6

    /** Keys whose local value differs from what was last seen synced. */
    fun dirty(local: Map<String, Double>, seen: Map<String, Seen>): Set<String> =
        (local.keys + seen.keys).filter { !same(local[it], seen[it]?.v) }.toSortedSet()

    /** The PATCH for the unsent edits; empty when there is nothing to send. */
    fun push(local: Map<String, Double>, seen: Map<String, Seen>): Push {
        val keys = dirty(local, seen)
        return Push(keys.associateWith { local[it] }, keys.associateWith { seen[it]?.at })
    }

    /** Server readings merged into the local ones -- see the object's comment. */
    fun pull(local: Map<String, Double>, seen: Map<String, Seen>, remote: Map<String, Remote>): Merge {
        val actual = local.toMutableMap()
        val now = seen.toMutableMap()
        val taken = mutableListOf<String>()
        for ((k, r) in remote) {
            val lv = local[k]; val s = seen[k]
            if (same(lv, r.v)) { now[k] = Seen(r.v, r.at); continue }   // already agree: adopt the baseline
            if (!same(lv, s?.v)) continue                                // an unsent local edit stays
            if (s?.at == r.at) continue                                  // nothing new on the server
            if (r.v == null) actual.remove(k) else actual[k] = r.v
            now[k] = Seen(r.v, r.at); taken.add(k)
        }
        // Seen on the server once, gone from it now: deleted on the web.
        for ((k, s) in seen) {
            if (k in remote || s.at == null) continue
            if (same(local[k], s.v)) {
                if (local[k] != null) taken.add(k)
                actual.remove(k); now.remove(k)
            } else now[k] = Seen(s.v, null)                             // local edit kept; it is a new key now
        }
        return Merge(actual, now, taken.sorted(), emptyMap())
    }

    /**
     * The answer to a PATCH: [written] keys are now seen as sent; each of
     * [conflicts] takes the server value locally; then the full [readings]
     * map is pulled, so anything else the web changed arrives too.
     */
    fun afterPush(local: Map<String, Double>, seen: Map<String, Seen>, sent: Push,
                  written: Collection<String>, conflicts: Map<String, Remote>,
                  readings: Map<String, Remote>): Merge {
        val actual = local.toMutableMap()
        val now = seen.toMutableMap()
        for (k in written) {
            if (k !in sent.set) continue
            val v = sent.set[k]
            if (v == null) now.remove(k) else now[k] = Seen(v, readings[k]?.at)
        }
        for ((k, r) in conflicts) {
            if (r.v == null) actual.remove(k) else actual[k] = r.v
            if (r.v == null) now.remove(k) else now[k] = Seen(r.v, r.at)
        }
        val m = pull(actual, now, readings)
        return Merge(m.actual, m.seen, m.taken, conflicts)
    }

    /**
     * A CONFLICT ON A READING WHOSE MARK IS GONE HERE (v52 slice 8 review). Delete or a clear takes FW1 and its
     * readings; the push sends {FW1.w: null}; if staff changed FW1.w on the web meanwhile, the server answers a
     * conflict, and applying it as-is would put FW1.w back with no FW1 to show it under -- an orphan the phone can
     * neither display nor remove, which the next ↶ (keeping only readings with a mark) would then delete on the
     * web without anybody asking. The rule, made on purpose: THE PHONE'S DELETE STANDS. Each [applied] conflict
     * whose key is not [live] (its mark is gone) is kept out of [actual], and [seen] takes the server's value and
     * new `at`, so the key is an unsent delete based on the latest `at` -- the next push sends it once and it is
     * written. A conflict the server answered with a delete needs nothing. Returns the keys re-deleted, sorted,
     * for the screen to say ([orphanText]).
     */
    fun orphans(actual: MutableMap<String, Double>, seen: MutableMap<String, Seen>,
                applied: Map<String, Remote>, live: (String) -> Boolean): List<String> {
        val out = mutableListOf<String>()
        for ((k, r) in applied) {
            if (live(k)) continue
            actual.remove(k)
            val v = r.v
            if (v == null) seen.remove(k) else { seen[k] = Seen(v, r.at); out.add(k) }
        }
        return out.sorted()
    }

    /** What the screen says about [orphans]' keys, or null when there are none (keyed in port 61c slice M3b). */
    fun orphanText(tx: Tx, keys: List<String>): String? =
        if (keys.isEmpty()) null
        else keys.joinToString("; ") { tx.sp_x_changed_web_mark_deleted(it) } + tx.sp_deleted_web_too // text:value the list separator

    /**
     * WHAT A PUSH'S CONFLICTS SAY (moved here from ReadingsSync in port 61c slice M3b, so the :pano suite can pin the
     * rendering under both Tx -- ReadingsSync is :app and no :pano test can reach it; the M3b critic's finding 4).
     * The PULL path (survey prep) renders with the screen's tx; the PUSH path's stored note composes with Tx.EN until
     * M2 (ReadingsSync.push).
     */
    fun conflictText(tx: Tx, conflicts: Map<String, Remote>): String =
        conflicts.entries.sortedBy { it.key }.joinToString("; ") { (k, r) -> // text:value the list separator
            if (r.by.isNotBlank()) tx.sp_x_changed_web_by_x(code = k, by = r.by) else tx.sp_x_changed_web(k) }

    /** conflictText's own two EN shapes, matched back by [conflictNoteLine]. Storage never sees Marathi. */
    private const val CHANGED_BY_EN = " changed on the web by " // text:value conflictText's EN shape, parsed back, never shown raw
    private const val CHANGED_EN = " changed on the web" // text:value conflictText's EN shape, parsed back, never shown raw

    /**
     * THE PUSH PATH'S STORED CONFLICT NOTE, RE-RENDERED AT DRAW TIME (port 61c slice M2). The note
     * ReadingsSync.push stores is `conflictText(Tx.EN, …)` — storage never sees Marathi, and old
     * state files already hold those bytes — and this draws it in the live language: split on
     * "; ", each segment matched against conflictText's own two EN shapes (`{code} changed on the
     * web`, `… by {by}`) and rendered through the same keys, `{by}` as given; a segment that
     * matches neither is drawn as stored. Owner and parser are ONE file, pinned by a round trip:
     * `conflictNoteLine(tx, conflictText(Tx.EN, c)) == conflictText(tx, c)` for every shape,
     * under both Tx (ReadingsTest).
     */
    fun conflictNoteLine(tx: Tx, note: String): String =
        note.split("; ").joinToString("; ") { seg -> // text:value the list separator
            val by = seg.indexOf(CHANGED_BY_EN)
            when {
                by > 0 -> tx.sp_x_changed_web_by_x(
                    code = seg.substring(0, by), by = seg.substring(by + CHANGED_BY_EN.length))
                seg.endsWith(CHANGED_EN) && seg.length > CHANGED_EN.length ->
                    tx.sp_x_changed_web(seg.removeSuffix(CHANGED_EN))
                else -> seg
            }
        }

    /**
     * THE READINGS LINE'S COLOUR CLASS, decided by KIND, never by string equality (port 61c slice M3b, critic note 3:
     * a string compare against a now-bilingual text is the two-answers fault in miniature). OK (green) only when the
     * pull SUCCEEDED and nothing had to be said -- a conflict or an orphan keeps the line amber even on a successful
     * pull, which is today's behaviour exactly (the M3b critic's finding 4); NOT_YET (grey) only when clean too.
     */
    enum class NoteKind { OK, NOT_YET, FAILED }

    fun noteKind(ok: Boolean, notYet: Boolean, clean: Boolean): NoteKind = when {
        !clean -> NoteKind.FAILED
        ok -> NoteKind.OK
        notYet -> NoteKind.NOT_YET
        else -> NoteKind.FAILED
    }
}
