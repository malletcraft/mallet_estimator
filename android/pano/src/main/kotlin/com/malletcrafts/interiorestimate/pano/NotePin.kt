package com.malletcrafts.interiorestimate.pano

import kotlin.math.min

/**
 * THE NOTE'S PIN (port 62 slice L6, 12 Nov 2026; the approved prototype v61c.2, index61c2.html, module L53: noteTag
 * 2914-2917, paintNote 2918-2926). Amit, 16 Oct 2026, 06:37 IST: "Need a note annotation for simple note marking."
 *
 * A Note is a mark that is ONLY WORDS: no box, no measures, no state. On a face -- the squared photo and the 360 -- it
 * is a 20 dp white disc ringed in ink with ✎ on it, and beside it its tag: its code and ✎ ("FNOTE1 ✎"), a small
 * padlock when the note is for the OFFICE ONLY, then the first [TAG_CHARS] characters of its words, the whole tag
 * never wider than [TAG_MAX] dp. Picked, the disc takes a gold ring. It is pressed and moved like a spot
 * ([SurveyPrep.isSpot]) and has no tabs.
 *
 * PURE: [tag] is what the tag says and how wide it is, with the text's width handed in as a function (the painter's
 * own measure of an 11 u bold line), so the cut is tested here against the page's own noteTag; the sizes are the
 * page's, in u (dp on the canvas). THE PIN AND ITS TAG ARE DRAWN ON THE SCREEN ONLY: a picture or a sheet for anybody
 * else is handed the room as surveyed ([SheetMarks]), which holds no Note, so no painter can write an office-only
 * note's words into a document (DESIGN-port62 section 3.6; the plan critic's finding 3).
 */
object NotePin {

    /** The disc's radius, its ink ring, and how far outside the disc the picked ring is drawn, u. */
    const val R = 10.0
    const val RING = 2.0
    const val SEL_GAP = 3.0
    /** The dot a Note is in the magnifier: 4 u, white. */
    const val LOUPE_R = 4.0

    /** The ✎ on the disc: bold 12 u, centred half a u under the disc's centre. */
    const val GLYPH = 12.0
    const val GLYPH_DY = 0.5

    /** The tag: bold 11 u, starting 4 u right of the disc on a baseline 4 u under its centre, on a 3 u dark halo. */
    const val TAG_SIZE = 11.0
    const val TAG_DX = 4.0
    const val TAG_DY = 4.0
    const val HALO = 3.0
    /** Its box for a press: from 11 u above the baseline, 14 u tall. */
    const val TAG_UP = 11.0
    const val TAG_H = 14.0

    /** The tag is never wider than this, u; its words are the note's first 24 characters before any cut. */
    const val TAG_MAX = 120.0
    const val TAG_CHARS = 24

    /** An office-only Note's padlock: 10 u across, its centre 5 u right of the head and 4 u above the baseline, in 12 u of room. */
    const val LOCK = 10.0
    const val LOCK_DX = 5.0
    const val LOCK_UP = 4.0
    const val LOCK_W = 12.0
    /** After the head: 2 u, then the padlock's room, then a space's width less 2 u before the words. */
    const val HEAD_GAP = 2.0
    const val TEXT_BACK = 2.0

    /** A locked Note's padlock pill: its top-right corner 14 u right of the pin's centre and 26 u above it. */
    const val PAD_DX = 14.0
    const val PAD_UP = 26.0

    /** The disc, its ring and glyph, the picked ring, the padlock and the words (0xRRGGBB). */
    const val DISC = 0xffffff
    const val INK = 0x1d1f1a
    const val PICKED = 0xE0B040
    const val LOCK_COLOUR = 0xFFC46B
    const val TEXT_COLOUR = 0xECEEE8

    private const val CUT = "…" // text:value a glyph: what was cut, the same in both languages

    /**
     * A Note's tag: [head], "FNOTE1 ✎"; [text], its words on one line, cut -- "" for a Note with none; [office], the
     * padlock is drawn between them; [width] of the whole tag on the canvas, never over [TAG_MAX] u.
     */
    class Tag(val head: String, val text: String, val office: Boolean, val width: Double)

    /** JavaScript's `\s`: every Unicode space and line end, and the byte-order mark. */
    private fun space(c: Char): Boolean = c.isWhitespace() || c == '﻿'

    /** The note on ONE line (`replace(/\s+/g,' ').trim()`): each run of spaces and line ends one space, none at its ends. */
    fun oneLine(note: String): String {
        val out = StringBuilder(note.length); var gap = false
        for (c in note) {
            if (space(c)) { gap = out.isNotEmpty(); continue }
            if (gap) { out.append(' '); gap = false }
            out.append(c)
        }
        return out.toString()
    }

    /** [t] without its last character -- nor half of one: a pair's high half left at the end goes with it. */
    private fun dropLast(t: String): String {
        val cut = t.dropLast(1)
        return if (cut.isNotEmpty() && cut.last().isHighSurrogate()) cut.dropLast(1) else cut
    }

    /**
     * THE TAG OF THE NOTE [e] (noteTag): its code and ✎; then its words on one line -- the first [TAG_CHARS]
     * characters and an ellipsis when there are more -- cut further, a character at a time, until the head, the
     * padlock's room and the words together are no wider than [TAG_MAX] u (down to the ellipsis alone). [u]: canvas
     * px per dp; [measure]: the width on the canvas of a text in the tag's own font, bold [TAG_SIZE] u.
     *
     * The page cuts by UTF-16 units and can leave half an emoji at the cut; here a cut never halves one -- the same
     * words in every other case.
     */
    fun tag(e: SurveyPrep.Mark, u: Double, measure: (String) -> Double): Tag {
        val head = e.code + " " + MarkLife.NOTE_BADGE
        val words = oneLine(MarkLife.noteOf(e.extra))
        val office = MarkLife.isOffice(e.extra)
        val max = TAG_MAX * u
        val base = measure("$head ") + if (office) LOCK_W * u else 0.0
        var t = if (words.length > TAG_CHARS) words.substring(0, TAG_CHARS).let { if (it.last().isHighSurrogate()) it.dropLast(1) else it } + CUT else words
        while (t.isNotEmpty() && base + measure(t) > max) {
            t = dropLast(t.removeSuffix(CUT)).trimEnd() + CUT
            if (t == CUT) break
        }
        return Tag(head, t, office, min(max, base + if (t.isNotEmpty()) measure(t) else 0.0))
    }
}
