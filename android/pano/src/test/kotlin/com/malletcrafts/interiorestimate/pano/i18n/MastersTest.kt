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
import com.malletcrafts.interiorestimate.pano.WireCleanProbe
import com.malletcrafts.interiorestimate.pano.WireLeakProbe
import com.malletcrafts.interiorestimate.pano.WishList
import java.io.File
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        WishList.SERVICES.first { it.id == "plumbing" }.detail to "the plumbing service's description: its key landed in L2 (wish.water_drain_points_taps); its masters.tsv row, and this entry's removal, are Marathi slice 4 (port M4)",
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

    private val i18nPkg = Tx::class.java.packageName

    /** [t] is, or is built from, a type of the i18n package: `Tx`, `Msg` (`Function1<Tx, String>`), `List<Msg>`… */
    private fun i18nType(t: Type): Boolean = when (t) {
        is Class<*> -> if (t.isArray) i18nType(t.componentType) else t.name.startsWith("$i18nPkg.")
        is ParameterizedType -> i18nType(t.rawType) || t.actualTypeArguments.any(::i18nType) || (t.ownerType?.let(::i18nType) == true)
        is WildcardType -> t.upperBounds.any(::i18nType) || t.lowerBounds.any(::i18nType)
        is GenericArrayType -> i18nType(t.genericComponentType)
        is TypeVariable<*> -> t.bounds.any(::i18nType)
        else -> false
    }

    /** A parameter or the result names an i18n type, generics included (an erased `Msg` is only `Function1`). */
    private fun takesI18n(m: Method) = m.genericParameterTypes.any(::i18nType) || m.parameterTypes.any(::i18nType) || i18nType(m.genericReturnType)

    /**
     * The compiled class files of [c] and of every class nested in it (`C$…`) that name the i18n package
     * anywhere -- a `Tx.MR.x` read in a body, a `Masters` call, a `Msg` in a signature. A body can reach
     * Marathi with no i18n parameter at all, so for an object whose every method builds wire or export
     * text the bytes are the check, not the signatures.
     */
    private fun bytecodeNamesI18n(c: Class<*>): List<String> {
        val root = File(c.protectionDomain.codeSource.location.toURI())
        val dir = root.resolve(c.name.substringBeforeLast('.').replace('.', '/'))
        val files = dir.listFiles { f -> f.name == "${c.simpleName}.class" || (f.name.startsWith("${c.simpleName}$") && f.name.endsWith(".class")) }.orEmpty()
        require(files.any { it.name == "${c.simpleName}.class" }) { "no class file for ${c.name} in $dir" }
        val needle = (i18nPkg.replace('.', '/') + "/").toByteArray(Charsets.US_ASCII)
        fun has(b: ByteArray) = (0..b.size - needle.size).any { i -> needle.indices.all { b[i + it] == needle[it] } }
        return files.filter { has(it.readBytes()) }.map { it.name }.sorted()
    }

    /**
     * THE WIRE, STORAGE AND EXPORT BUILDERS TAKE NO Tx, so they cannot become Marathi (the phone's
     * ct57-8). A whole object where every method builds wire or export text: its compiled bytes may not
     * name the i18n package at all. A named method where the object also composes screen text that WILL
     * take a Tx in its slice: no parameter or result of an i18n type, generics included.
     */
    @Test fun `the wire, storage and export builders have no Tx parameter`() {
        val whole = listOf(Handover::class.java, SurveyExport::class.java, SvgWriter::class.java)
        val named = mapOf(
            // afterFinal, afterSurvey and left (port 61c slice W) write the stored list: S5's words stay as S5 sent them
            WishList::class.java to listOf("forPut", "normalised", "edited", "afterPut", "afterFinal", "afterSurvey", "left"),
            Album::class.java to listOf("provisional", "provisionalPhoto"),
            SurveyPrepFile::class.java to listOf("encode"),
        )
        val off = ArrayList<String>()
        for (c in whole) {
            c.declaredMethods.filter(::takesI18n).forEach { off += "${c.simpleName}.${it.name}: an i18n type in its signature" }
            bytecodeNamesI18n(c).forEach { off += "${c.simpleName}: $it names the i18n package (Tx, Masters or Msg)" }
        }
        for ((c, names) in named) for (n in names) {
            val ms = c.declaredMethods.filter { it.name == n }
            if (ms.isEmpty()) off += "${c.simpleName}.$n: no such method (update this list with the builder's new name)"
            ms.filter(::takesI18n).forEach { off += "${c.simpleName}.${it.name}: an i18n type in its signature" }
        }
        I18n.check("wire has no Tx", off, "a wire, storage or export builder took a Tx or an i18n type: what is stored or sent must stay English")
    }

    @Test fun `the wire check sees a Msg parameter and a Tx read in a body`() {
        val leak = WireLeakProbe::class.java
        assertTrue(takesI18n(leak.declaredMethods.single { it.name == "shown" }), "a Msg parameter erases to Function1 and must still be seen")
        assertTrue(bytecodeNamesI18n(leak).isNotEmpty(), "a body reading Tx.MR has no i18n parameter and must still be seen")
        assertEquals(emptyList(), WireCleanProbe::class.java.declaredMethods.filter(::takesI18n).map { it.name })
        assertEquals(emptyList(), bytecodeNamesI18n(WireCleanProbe::class.java))
    }
}
