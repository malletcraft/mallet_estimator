package com.malletcrafts.interiorestimate.pano.i18n

import com.malletcrafts.interiorestimate.pano.Album
import com.malletcrafts.interiorestimate.pano.AlbumQueue
import com.malletcrafts.interiorestimate.pano.AlbumWire
import com.malletcrafts.interiorestimate.pano.AsIsPath
import com.malletcrafts.interiorestimate.pano.FlatPresets
import com.malletcrafts.interiorestimate.pano.Handover
import com.malletcrafts.interiorestimate.pano.MarkLife
import com.malletcrafts.interiorestimate.pano.PhaseTools
import com.malletcrafts.interiorestimate.pano.Phases
import com.malletcrafts.interiorestimate.pano.QuoteMove
import com.malletcrafts.interiorestimate.pano.QuoteStage
import com.malletcrafts.interiorestimate.pano.QuoteView
import com.malletcrafts.interiorestimate.pano.SheetMarks
import com.malletcrafts.interiorestimate.pano.SurveyExport
import com.malletcrafts.interiorestimate.pano.SurveyPlan
import com.malletcrafts.interiorestimate.pano.SurveyPrep
import com.malletcrafts.interiorestimate.pano.SurveyPrepFile
import com.malletcrafts.interiorestimate.pano.SurveyPrepPalette
import com.malletcrafts.interiorestimate.pano.SvgWriter
import com.malletcrafts.interiorestimate.pano.Trades
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
            Triple("SurveyPlan.VIEWS", Masters::surface_tab, SurveyPlan.VIEWS.map { it.second }),
            Triple("SurveyPlan.DTITLE", Masters::view_title, SurveyPlan.DTITLE.values),
            Triple("Handover.FACE_LABELS", Masters::surface_tab, Handover.FACE_LABELS.values),
            Triple("SurveyPrepPalette.SHORT", Masters::type_short, SurveyPrepPalette.SHORT.values.distinct()),
            Triple("WishList.SERVICES", Masters::service, WishList.SERVICES.map { it.name }),
            Triple("WishList.SERVICES notes", Masters::service_note, WishList.SERVICES.map { it.detail }.filter { it.isNotEmpty() }),
            Triple("the default site", Masters::site_name, listOf("Main site")),
            // S5Masters.NO_CLIENT (an :app constant this module cannot read): the stored bucket
            // value byte for byte, through the map masters.tsv gained in port 61c slice M6
            Triple("S5Masters.NO_CLIENT", Masters::client_name, listOf("(no client)")),
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
        // port 61c slice AQ: the album outbox's requests and S5's answers -- every method builds or reads what is sent;
        // port 61c slice AS1: the as-is upload's capture addresses and its record body, every method a wire value.
        // M3c moves SurveyExport from whole to named: sheet() now PINS the drawing's language by passing the literal
        // Tx.EN (decision 4), so its bytes name the i18n package by design -- check (h) scans the file whole and its
        // companion pins the argument; what stays here is that no SIGNATURE takes an i18n type, so no caller can ever
        // hand the sheet a live language
        // port 62 slice L2: SheetMarks decides which marks a picture that LEAVES the phone draws (FaceWriter's saved
        // "survey marks"), and what of them -- an export's content, so the whole object names no language at all
        val whole = listOf(Handover::class.java, SvgWriter::class.java, AlbumQueue::class.java, AlbumWire::class.java,
            AsIsPath::class.java, SheetMarks::class.java)
        val named = mapOf(
            SurveyExport::class.java to listOf("footSource", "sheetScale", "safePart", "exportName", "shareExt",
                "keepDir", "roomToken", "drawn", "sheet"),
            // afterFinal, afterSurvey and left (port 61c slice W), and after409, shown and entered (its review), write the
            // stored list: S5's words stay as S5 sent them. Port M4 adds the two that write the stored notes (REPLACED,
            // NOT_SENT) and s5Answered, the stored reason: English in storage, drawn through their keys by noteLine
            WishList::class.java to listOf("forPut", "normalised", "edited", "afterPut", "afterFinal", "afterSurvey", "left",
                "after409", "shown", "entered", "afterConflict", "afterRefused", "s5Answered"),
            // port 61c slice K1b: the filing, the ids, the ops, the scope amendment, the log pages and every outbox
            // transition build what is stored or sent; only Refusal.text, CrKind.word/sub, CrOutcome.text and the
            // display functions (workName, payOf, dayOf, whenOf, decidedText, roleLabel...) take a Tx; AQ adds refileOp (S5-P)
            Album::class.java to listOf("provisional", "provisionalPhoto", "workOfCapture", "workOfStage", "mintId", "baseOf",
                "stateOp", "crRaiseOp", "crAnswerOp", "reviewOp", "photoOp", "amendedScope", "crDone", "mergedLog", "paged",
                "merged", "statesOfWorks", "statesOfLog", "withQueued", "afterPost", "afterReplay", "afterConflict",
                "afterLocked", "afterRefused", "refileOp", "nextPage"),
            SurveyPrepFile::class.java to listOf("encode"),
            // port 61c slice QM0: the guarded quote move builds what is sent and reads S5's answers into what is stored
            // (only text and localText take a Tx, for the status line) ...
            QuoteMove::class.java to listOf("stage", "scope", "answer", "scopeAnswer", "copyOf", "nowIn", "worksOf", "notOnS5"),
            // ... and the one project-state store keeps S5's words in English: every reader and merge of its copy (only
            // projectLine and coverageParts take a Tx, to draw); port 61c slice PL adds its stored shape and its one writer
            QuoteStage::class.java to listOf("merge", "mergeStates", "fromAlbum", "listedOf", "fromList", "takes", "named",
                "roomDot", "surveyShot", "coverage", "stored", "storedOf", "writes"),
            // port 61c slice QM1: the strip's segment-to-stage mapping and tap rule decide what is SENT (the stage an
            // identity value); only the texts (all, segments, doesLine, ask, the rows, approve) take a Tx; QM2 adds
            // needsMirror (which stages read the album)
            Phases::class.java to listOf("stageOf", "phaseOf", "tap", "needsMirror"),
            // port 61c slice QM2: the scope sheet's ticks -- what it opens with (and the stamp beside them, the base a submit
            // SENDS: open, its review), a tap, and what its submit SENDS -- hold S5's work ids; only the texts (title, note,
            // countLine, the labels, scopeRow) take a Tx
            QuoteView::class.java to listOf("openTicks", "open", "toggled", "submit", "rows"),
            // port 62 slice L1: a mark's life keys are STORED in the prep file -- the state, what it was born, the act, who
            // decided, the stamp, the why, the note -- so every reader and writer of them takes no Tx; only the names, the
            // words on a move's button and the refusals do
            // (slice L2 adds what the painter reads of the stored state without a word: the widths and the label's bar;
            // only tag, which names the state on its pill, takes a Tx)
            MarkLife::class.java to listOf("lifeOf", "bornOf", "actOf", "whyOf", "noteOf", "allowedMoves", "move", "restore", "placeAs",
                "setAct", "withNote", "setNote", "istIso", "countLife", "addCounts", "rollUp", "outlineWidth", "ringWidth", "barColour",
                "deleteAct", "spared", "unlockable"),
            // ... a mark's trade is its stored cat or its type's own; only name and short take a Tx
            Trades::class.java to listOf("tradeOf", "of", "catOf", "subOf", "withCat", "setCat", "ownOn", "seesOn", "moreRows"),
            // ... and what a phase shows, offers and places decides what is WRITTEN (asFor: the `born` a new mark gets); only
            // the band, the gate's line and the refusals take a Tx
            PhaseTools::class.java to listOf("stageOf", "can", "steps", "modeFor", "toolFor", "asFor", "asChip", "nextAs", "moveOk", "gate",
                "editable", "delOk", "lockOk", "bulk"),
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
