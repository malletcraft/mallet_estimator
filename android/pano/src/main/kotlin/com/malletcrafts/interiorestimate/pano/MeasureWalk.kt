package com.malletcrafts.interiorestimate.pano

/**
 * THE WALK THROUGH WHAT IS STILL TO MEASURE, AND ITS COUNTS (port 63 slice M1, 16 Nov 2026; DESIGN-port63 §1.5). Amit,
 * 15 Nov 2026: "The laundry list of measurements shown below screen is also frustrating." The list goes (slice M2);
 * what it did for him -- every reading box of a face, one after the other -- is this walk: 3 · Measure makes the next
 * unmeasured dimension active by itself after every reading, `›` skips one and comes round to it again, `‹` goes back
 * to correct one, and the bar and the face tabs say how many are left.
 *
 * ONE RULE FOR ALL SIX SURFACES. The stops of a face, in order:
 *  1. the marks that are asked ([MarkDims.asked]: in the room, and not a Note), by their left edge `x0` rounded to
 *     the whole mm, ascending; equal `x0`: `y0` ascending (the lower first); then by code. A point is ordered by its
 *     box like any mark;
 *  2. inside a mark, the order [SurveyPrep.measures] gives;
 *  3. then the Lines and areas, in stored order (a tape has no reading);
 *  4. then each chain's [SurveyChain.openKeys], in stored order -- its segments, then its total, the total left out
 *     where the laser gives it.
 * Notes, tapes and marks not in the room are left out. Faces follow the tabs ([FACES]).
 *
 * WHAT COUNTS AS LEFT ([left]): a stop with no reading -- and, for a chain's key, no figure from [SurveyChain.vals]
 * either. A chain is used to read n - 1 segments and derive the last, and with every segment read its total is
 * derived; neither has a reading BY DESIGN. So a derived segment and a derived total are not left: [next] skips them,
 * they are in no count, and "7 of 18 measured" counts neither side of them ([Count.read] is the stops with a reading,
 * [Count.total] is that plus what is left), so "all N measured" is true of N readings. Each is still a stop: a tap or
 * [previous] reaches it, and it takes a typed or lasered reading. A total the laser gives is not a stop at all.
 *
 * Pure, and it writes nothing: progress never gates Save. The reading keys are exactly [SurveyPrep.Prep.keysOf]'s.
 */
object MeasureWalk {

    /** The faces in the order of their tabs: front, right, back, left, floor, ceiling. */
    val FACES: List<String> = SurveyPrep.SURFACES

    /** The marks of a face that are asked, in the walk's order (rule 1). */
    fun marks(prep: SurveyPrep.Prep, s: String): List<SurveyPrep.Mark> =
        (prep.surfaces[s]?.marks ?: emptyList<SurveyPrep.Mark>()).filter { MarkDims.asked(it) }
            .sortedWith(compareBy<SurveyPrep.Mark> { Math.round(it.x0) }.thenBy { it.y0 }.thenBy { it.code })

    /** EVERY READING KEY THE FACE [s] ([a] x [b] mm) ASKS, in the walk's exact order. */
    fun stops(prep: SurveyPrep.Prep, s: String, a: Double, b: Double): List<String> {
        val sf = prep.surfaces[s] ?: return emptyList()
        return marks(prep, s).flatMap { prep.keysOf(s, it, a, b) } +
            sf.meas.flatMap { prep.keysOf(it) } +
            sf.chains.flatMap { SurveyChain.openKeys(it, a, b) }
    }

    /** Whether the stop [code] of [sf] is left, without asking again whether it is a stop. */
    private fun open(prep: SurveyPrep.Prep, sf: SurveyPrep.Surface, a: Double, b: Double, code: String): Boolean {
        if (prep.actual[code] != null) return false
        val c = sf.chains.firstOrNull { code.startsWith(it.code + ".") } ?: return true
        val v = SurveyChain.vals(c, prep.actual, a, b)
        val key = code.substring(c.code.length + 1)
        if (key == SurveyChain.TOTAL) return v.total == null
        val i = key.toIntOrNull() ?: return true
        return v.value.getOrNull(i - 1) == null
    }

    /**
     * WHETHER THE READING [code] IS STILL TO TAKE on the face [s]: it is one of the face's [stops], it has no reading,
     * and -- a chain's key -- [SurveyChain.vals] gives it no figure either (a derived segment and a derived total are
     * not left). False for a key that is no stop of this face.
     */
    fun left(prep: SurveyPrep.Prep, s: String, a: Double, b: Double, code: String): Boolean {
        val sf = prep.surfaces[s] ?: return false
        return code in stops(prep, s, a, b) && open(prep, sf, a, b, code)
    }

    /**
     * THE NEXT STOP THAT IS LEFT after [after], WRAPPING -- so a dimension skipped with `›` comes round again, and the
     * only one left is its own next. [after] null (or no stop of this face any more): the first one left. Null when
     * nothing on the face is left.
     */
    fun next(prep: SurveyPrep.Prep, s: String, a: Double, b: Double, after: String?): String? {
        val sf = prep.surfaces[s] ?: return null
        val ks = stops(prep, s, a, b)
        val i = if (after != null) ks.indexOf(after) else -1
        return (ks.drop(i + 1) + ks.take(i + 1)).firstOrNull { open(prep, sf, a, b, it) }
    }

    /**
     * THE STOP BEFORE [before], READ OR NOT, WRAPPING: how a reading already taken is reached to correct it. [before]
     * null (or no stop of this face any more): the LAST stop of the face, so a finished face can still be walked
     * back into. Null when the face asks nothing.
     */
    fun previous(prep: SurveyPrep.Prep, s: String, a: Double, b: Double, before: String?): String? {
        val ks = stops(prep, s, a, b)
        if (ks.isEmpty()) return null
        val i = if (before != null) ks.indexOf(before) else -1
        return if (i <= 0) ks.last() else ks[i - 1]
    }

    /** A face's (or the room's) progress: [read] stops with a reading, of [total] -- [read] plus what is [left]. */
    class Count(val read: Int, val total: Int) {
        /** What is still to measure. */
        val left: Int get() = total - read
        /** Something was asked, and none of it is left. */
        val done: Boolean get() = total > 0 && left == 0
    }

    /** The progress of the face [s]: "7 of 18 measured". A derived chain figure is on neither side. */
    fun count(prep: SurveyPrep.Prep, s: String, a: Double, b: Double): Count {
        val sf = prep.surfaces[s] ?: return Count(0, 0)
        val ks = stops(prep, s, a, b)
        val read = ks.count { prep.actual[it] != null }
        return Count(read, read + ks.count { open(prep, sf, a, b, it) })
    }

    /** The whole room's progress, every face added; [ab] gives a face's size, across and up. */
    fun room(prep: SurveyPrep.Prep, ab: (String) -> DoubleArray): Count {
        var read = 0; var total = 0
        for (s in FACES) { val z = ab(s); val c = count(prep, s, z[0], z[1]); read += c.read; total += c.total }
        return Count(read, total)
    }

    /** The next face to measure: which, and how many are [left] there. */
    class Face(val s: String, val left: Int)

    /**
     * THE NEXT FACE IN TAB ORDER WITH SOMETHING LEFT, after [from] and wrapping, [from] itself never: what a finished
     * face offers ("Next: Right wall · 4 to measure"). Null when no other face has anything left. The screen never
     * changes face by itself -- the engineer has to turn round.
     */
    fun nextFace(prep: SurveyPrep.Prep, from: String, ab: (String) -> DoubleArray): Face? {
        val i = FACES.indexOf(from)
        for (s in FACES.drop(i + 1) + FACES.take(maxOf(i, 0))) {
            val z = ab(s); val n = count(prep, s, z[0], z[1]).left
            if (n > 0) return Face(s, n)
        }
        return null
    }

    // ------------------------------------------------------------------ inside the picked item (2 · Mark, 4 · 360)

    /**
     * THE PICKED ITEM'S OWN KEYS, in order: a mark's [SurveyPrep.Prep.keysOf] (none for a mark that is not asked, which
     * is never active), a Line's or an area's one key (a tape: none), a chain's [SurveyChain.openKeys]. In 2 · Mark
     * and 4 · 360 `‹` and `›` move within these alone ([step]) and never change the selection.
     */
    fun stopsOf(prep: SurveyPrep.Prep, id: String, ab: (String) -> DoubleArray): List<String> {
        prep.markById(id)?.let { (s, e) ->
            if (!MarkDims.asked(e)) return emptyList()
            val z = ab(s); return prep.keysOf(s, e, z[0], z[1])
        }
        prep.measById(id)?.let { (_, m) -> return prep.keysOf(m) }
        prep.chainById(id)?.let { (s, c) -> val z = ab(s); return SurveyChain.openKeys(c, z[0], z[1]) }
        return emptyList()
    }

    /**
     * The key after ([forward]) or before [from] in [keys], READ OR NOT, WRAPPING. [from] null or not one of them:
     * the first going forward, the last going back. An item with one key answers that key (the bar draws no `‹` `›`
     * for it); null only when there are none.
     */
    fun step(keys: List<String>, from: String?, forward: Boolean): String? {
        if (keys.isEmpty()) return null
        val i = if (from != null) keys.indexOf(from) else -1
        if (i < 0) return if (forward) keys.first() else keys.last()
        return keys[(i + (if (forward) 1 else keys.size - 1)) % keys.size]
    }
}
