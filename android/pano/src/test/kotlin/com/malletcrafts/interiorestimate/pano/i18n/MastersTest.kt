package com.malletcrafts.interiorestimate.pano.i18n

import com.malletcrafts.interiorestimate.pano.Album
import com.malletcrafts.interiorestimate.pano.FlatPresets
import com.malletcrafts.interiorestimate.pano.Handover
import com.malletcrafts.interiorestimate.pano.QuoteStage
import com.malletcrafts.interiorestimate.pano.SurveyExport
import com.malletcrafts.interiorestimate.pano.SurveyPrep
import com.malletcrafts.interiorestimate.pano.SurveyPrepFile
import com.malletcrafts.interiorestimate.pano.SurveyPrepPalette
import com.malletcrafts.interiorestimate.pano.SvgWriter
import com.malletcrafts.interiorestimate.pano.WishList
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * STORAGE NEVER SEES MARATHI (design-marathi §2.6). Identity values stay English constants byte for
 * byte; [Masters] only SHOWS them. These tests are hand-typed: a literal is the point.
 */
class MastersTest {

    @Test fun `a room is shown in the language, stored in English`() {
        assertEquals("किचन", Masters.room(Tx.MR, "Kitchen"))
        assertEquals("Kitchen", Masters.room(Tx.EN, "Kitchen"))
        assertEquals("हॉल", Masters.room(Tx.MR, "Living Room"))
        // a typed name, or a master the map does not hold, is shown as typed
        assertEquals("typed name", Masters.room(Tx.MR, "typed name"))
        assertEquals("Ceiling", Masters.workphase(Tx.EN, "Ceiling"))
        // one English word, two maps, two answers: the homonym never has a second answer within a map
        assertEquals("सीलिंग", Masters.workphase(Tx.MR, "Ceiling"))
        assertEquals("छत", Masters.surface(Tx.MR, "Ceiling"))
    }

    /**
     * Identity values with no Marathi BY DECISION, each with its reason. Everything else in every
     * identity list must show Devanagari under MR.
     */
    private val PASS_THROUGH = mapOf(
        "CCTV" to "an abbreviation, shown as written",
        "Nos" to "a unit (latin-ok.txt)",
        "Top" to "Handover.FACE_LABELS: export-only, English (decision 4 of 21 Oct)",
        "Bottom" to "Handover.FACE_LABELS: export-only, English (decision 4 of 21 Oct)",
        WishList.SERVICES.first { it.id == "plumbing" }.detail to "the plumbing service's description needs a key: slice 4",
    )

    private val devanagari = Regex("[\\u0900-\\u097F]")

    @Test fun `every identity value has a Marathi name and shows its own bytes in English`() {
        val lists: List<Triple<String, (Tx, String) -> String, Collection<String>>> = listOf(
            Triple("QuoteStage.STAGES", Masters::quote, QuoteStage.STAGES + QuoteStage.APPROVED),
            Triple("Album.WORKS", Masters::work, Album.WORKS.map { it.name }),
            Triple("Album.STATES", Masters::state, Album.STATES),
            Triple("Album.STAGE_WORK", Masters::stage, Album.STAGE_WORK.keys),
            Triple("FlatPresets.TYPES", Masters::site, FlatPresets.TYPES),
            Triple("FlatPresets.rooms", Masters::room, FlatPresets.TYPES.flatMap { t -> (1..5).flatMap { FlatPresets.rooms(t, it) } }.distinct()),
            Triple("SurveyPrep.TYPES", Masters::type, SurveyPrep.TYPES.values.flatten().map { it.name }.distinct()),
            Triple("SurveyPrep.NAME", Masters::surface, SurveyPrep.NAME.values),
            Triple("SurveyPrep.TAB", Masters::surface_tab, SurveyPrep.TAB.values),
            Triple("Handover.FACE_LABELS", Masters::surface_tab, Handover.FACE_LABELS.values),
            Triple("SurveyPrepPalette.SHORT", Masters::type_short, SurveyPrepPalette.SHORT.values.distinct()),
            Triple("WishList.SERVICES", Masters::service, WishList.SERVICES.map { it.name }),
            Triple("WishList.SERVICES notes", Masters::service_note, WishList.SERVICES.map { it.detail }.filter { it.isNotEmpty() }),
            Triple("the default site", Masters::site_name, listOf("Main site")),
        )
        val off = ArrayList<String>()
        for ((list, show, values) in lists) for (v in values) {
            val en = show(Tx.EN, v); val mr = show(Tx.MR, v)
            if (en != v) off += "$list '$v': shows as '$en' in English (the key's English must be the stored value, byte for byte)"
            if (v in PASS_THROUGH) { if (mr != v) off += "$list '$v': on the pass-through list but shows '$mr'" }
            else if (!devanagari.containsMatchIn(mr)) off += "$list '$v': no Marathi name (add its key to android/i18n/masters.tsv, or the key to the prototype table)"
        }
        I18n.check("identity values", off, "every identity value is shown through Masters: English unchanged, Marathi present")
    }

    /**
     * THE WIRE, STORAGE AND EXPORT BUILDERS TAKE NO Tx, so they cannot become Marathi (the phone's
     * ct57-8). A whole object where every method builds wire or export text; a named method where the
     * object also composes screen text that WILL take a Tx in its slice.
     */
    @Test fun `the wire, storage and export builders have no Tx parameter`() {
        val whole = listOf(Handover::class.java, SurveyExport::class.java, SvgWriter::class.java)
        val named = mapOf(
            WishList::class.java to listOf("forPut", "normalised", "edited", "afterPut"),
            Album::class.java to listOf("provisional", "provisionalPhoto"),
            SurveyPrepFile::class.java to listOf("encode"),
        )
        val off = ArrayList<String>()
        fun tx(c: Class<*>) = c.name.startsWith(Tx::class.java.packageName + ".")
        for (c in whole) for (m in c.declaredMethods) if (m.parameterTypes.any(::tx)) off += "${c.simpleName}.${m.name}"
        for ((c, names) in named) for (n in names) {
            val ms = c.declaredMethods.filter { it.name == n }
            if (ms.isEmpty()) off += "${c.simpleName}.$n: no such method (update this list with the builder's new name)"
            ms.filter { m -> m.parameterTypes.any(::tx) }.forEach { off += "${c.simpleName}.${it.name}" }
        }
        I18n.check("wire has no Tx", off, "a wire, storage or export builder took a Tx or an i18n type: what is stored or sent must stay English")
    }
}
