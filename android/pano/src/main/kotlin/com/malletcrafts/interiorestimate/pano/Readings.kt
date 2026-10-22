package com.malletcrafts.interiorestimate.pano

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

    /** What the screen says about [orphans]' keys, or null when there are none. */
    fun orphanText(keys: List<String>): String? =
        if (keys.isEmpty()) null
        else keys.joinToString("; ") { "$it changed on the web after its mark was deleted here" } + " — deleted on the web too"
}
