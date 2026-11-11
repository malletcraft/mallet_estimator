package com.malletcrafts.interiorestimate.pano

/**
 * WHICH MARKS A DRAWING OF A FACE SHOWS, BY WHO IT IS FOR (port 62 slice L2, 11 Nov 2026; DESIGN-port62 section 3.2 and
 * the plan critic's finding 3).
 *
 * Survey prep's painter (the app's SurveyPrepDraw.drawMarks) has two kinds of caller. The screen is the OFFICE's:
 * staff see every mark with its state. FaceWriter's saved "<face> survey marks" pictures are for [Audience.OTHERS]:
 * they go to the gallery for the handover folder, "documents other parties consume". Drawn like the screen, every Save
 * would write a wish's state and act into that JPEG -- and, once the Note exists (slice L6), the first words of an
 * OFFICE-ONLY note. So the painter is HANDED its marks by [marks], and for anyone but the office that is THE ROOM AS
 * SURVEYED, until Amit rules what such a picture carries once wishes exist (DESIGN-port62 Q-list 7):
 *  - a mark born a Client wish or Vendor added is left out: it is not in the room;
 *  - a Note is left out, whatever its audience;
 *  - every other mark is given WITHOUT its state, act, why, note and audience -- so a surveyed window the client wants
 *    replaced, or a wish since Dropped, reads as the window that is there.
 * It is the rule S5 answers a vendor or a client login with (slice RD, DESIGN-port62 section 3.11), so outside the
 * office the phone's files and the web give one answer.
 *
 * AN ALLOW-LIST, NOT A DENY-LIST: [asSurveyed] keeps a mark's seven own fields and the keys [KEPT] names, and nothing
 * else. A key a later build adds to a mark is therefore office-only until this list names it; and since the painter
 * reads a mark's state from the mark it is handed, a sheet for others CANNOT draw one -- there is no second switch in
 * the painter to forget. `born` is read the same way: anything but absent or "exist" is not in the room, a value a
 * later build writes included.
 *
 * A Tape, a Line, an area and a chain are drawn as they are for every audience: no painter draws their note or
 * their trade, and they carry no state. Pure, and it takes no language: what an EXPORT draws is decided here.
 *
 * THE PLAN TAB'S A4 SHEET IS SUCH A DOCUMENT TOO (the L2 review, finding 1): SurveyExport.sheet is saved to Downloads
 * and shared, and SurveyPlan draws every mark it is handed as v52 does -- on the floor plan a door, a window or an
 * opening on a wall as A GAP THROUGH THE WALL BAND. Until the six drawings draw a wish as a wish (slice L8), a mark born
 * a wish would be on that sheet as if it were in the room. So the sheet is drawn from [prep] for [Audience.OTHERS]:
 * the room as surveyed, by the one rule above. The Plan tab ON SCREEN is the office's and draws the prep itself.
 */
object SheetMarks {

    /** Who a drawing of a face's marks is for. The painter takes it with NO default: every call site names its audience. */
    enum class Audience {
        /** The office: the screen staff work on. Every mark, with its state. */
        OFFICE,
        /** Anyone else: a saved picture, a document that leaves the phone. The room as surveyed. */
        OTHERS,
    }

    /**
     * The keys of a mark a sheet for others keeps besides its box: the lock, which moves nothing and says nothing, and
     * the sub-kind, which is what the thing IS. S5's own list for a caller who is not staff (slice RD): id, t, code,
     * x0, x1, y0, y1, locked, sub.
     */
    val KEPT: Set<String> = setOf(SurveyPrep.LOCKED, Trades.KEY_SUB)

    /** Whether the mark is IN THE ROOM AS SURVEYED: it was there (`born` absent or "exist") and it is not a Note. */
    fun surveyed(e: SurveyPrep.Mark): Boolean {
        if (MarkLife.isNote(e)) return false
        val born = e.extra[MarkLife.KEY_BORN]
        return born == null || born == MarkLife.EXIST
    }

    /**
     * The mark as it was surveyed: its id, type, code and box, and of its other keys only [KEPT] -- no state, stamp,
     * act, why, note or audience, and no key this build does not know. The mark itself when it carries nothing else
     * (every mark of a v52 prep the prototype wrote); else a NEW mark, so the prep is never changed.
     */
    fun asSurveyed(e: SurveyPrep.Mark): SurveyPrep.Mark =
        if (e.extra.keys.all { it in KEPT }) e
        else SurveyPrep.Mark(e.id, e.t, e.code, e.x0, e.x1, e.y0, e.y1, e.extra.filterKeys { it in KEPT })

    /**
     * The marks a drawing for [audience] shows, in the surface's own order: for the office [marks] as they are -- the
     * same list, so the screen's painter draws exactly what it drew; for others the [surveyed] ones, each [asSurveyed].
     */
    fun marks(marks: List<SurveyPrep.Mark>, audience: Audience): List<SurveyPrep.Mark> = when (audience) {
        Audience.OFFICE -> marks
        Audience.OTHERS -> marks.filter(::surveyed).map(::asSurveyed)
    }

    /**
     * The prep a DRAWING OF THE ROOM for [audience] is made from (the Plan tab's six drawings, SurveyPlan): for the
     * office [p] itself; for others [p] with each surface's marks replaced by [marks] -- the room as surveyed. A v52
     * prep, and any prep whose every mark is surveyed and carries no key but [KEPT], is answered as ITSELF, so its
     * sheet is what it always was. Else a NEW prep that SHARES everything but the lists of marks -- the lines, the
     * readings, the Tapes, Lines, areas and chains, the reference -- and is only ever READ: [p] is never changed, and
     * the readings of a mark that is left out stay in the map, where no drawing asks for them.
     */
    fun prep(p: SurveyPrep.Prep, audience: Audience): SurveyPrep.Prep {
        if (audience == Audience.OFFICE) return p
        val shown = p.surfaces.mapValues { (_, sf) -> marks(sf.marks, audience) }
        if (p.surfaces.all { (s, sf) -> shown.getValue(s).let { m -> m.size == sf.marks.size && m.indices.all { m[it] === sf.marks[it] } } }) return p
        val surfaces = LinkedHashMap<String, SurveyPrep.Surface>()
        for ((s, sf) in p.surfaces)
            surfaces[s] = SurveyPrep.Surface(sf.lines, sf.set, shown.getValue(s).toMutableList(), sf.extra, sf.meas, sf.measKey, sf.chains, sf.chainsKey)
        return SurveyPrep.Prep(surfaces, p.actual, p.extra, p.extraSurfaces)
    }

    /**
     * Whether a marked picture of the surface has anything on it for [audience]: a mark it shows ([marks]), a Tape, a
     * Line, an area or a chain. A surface that holds only wishes has no "survey marks" picture for others -- as a
     * surface with nothing marked never had one.
     */
    fun any(sf: SurveyPrep.Surface, audience: Audience): Boolean =
        marks(sf.marks, audience).isNotEmpty() || sf.meas.isNotEmpty() || sf.chains.isNotEmpty()
}
