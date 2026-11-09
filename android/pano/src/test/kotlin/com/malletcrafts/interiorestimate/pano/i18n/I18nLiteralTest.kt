package com.malletcrafts.interiorestimate.pano.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * EVERY TEXT A PERSON SEES IS KEYED, LISTED OR MARKED (design-marathi §3.3, I18nLiteralTest a–i).
 * Scans every module in [LiteralScan.ROOTS] -- `:app`, `:camera` and `:pano` main, the generated table
 * excepted. This is also the local stand-in for the compiler of `:app` and `:camera` (neither builds
 * here) on the commonest break, an unresolved name (f, and i for a :pano object's member): two of those broke `main` on 21 Sep.
 */
class I18nLiteralTest {

    private val android = I18n.android
    private val scans by lazy { LiteralScan.sources(android).map { LiteralScan.scan(android, it) } }
    private val unkeyed by lazy { LiteralScan.readTsv(I18n.i18n.resolve(LiteralScan.UNKEYED_TSV), "file") }
    private val ported by lazy {
        I18n.i18n.resolve(LiteralScan.PORTED_TXT).readLines().map { it.substringBefore('#').trim() }.filter { it.isNotEmpty() }
    }

    @Test fun `a every shown literal is keyed, listed in unkeyed tsv, or marked text value`() {
        val listed = unkeyed.toSet()
        val off = ArrayList<String>()
        for (s in scans) for (l in s.lits) {
            if (l.ui && LiteralScan.latinWord(l) && !s.marked(l) && (s.rel to l.cell) !in listed) off += "${s.rel}\t${l.cell}    (line ${l.line})"
        }
        I18n.check("a · unkeyed shown text", off,
            "a text a person sees: key it (tx.<accessor>), add the row to android/i18n/unkeyed.tsv, or mark its line `// text:value <why>`")
    }

    @Test fun `b the per-file count of prose literals is the ratchet's`() {
        val listed = LiteralScan.readTsv(I18n.i18n.resolve(LiteralScan.COUNTS_TSV), "file").associate { it.first to it.second.toInt() }
        val worklist = unkeyed.toSet()
        val off = ArrayList<String>()
        for (s in scans) {
            val prose = s.lits.filter { LiteralScan.counted(s, it, worklist) }
            val want = listed[s.rel] ?: 0
            if (prose.size > want) off += "${s.rel}: ${prose.size} prose literals, ${want} listed — a new one: key it, list it in unkeyed.tsv (a text a person sees), or mark its line `// text:value <why>` (a log line, a wire value); each lowers the count. " +
                "Its prose literals: " + prose.take(60).joinToString(" | ") { "${it.line}: ${it.cell.take(60)}" }
            if (prose.size < want) off += "${s.rel}: ${prose.size} prose literals, $want listed — run `gradle :pano:i18nRatchet`"
        }
        (listed.keys - scans.map { it.rel }.toSet()).forEach { off += "$it: in literal-counts.tsv but gone — run `gradle :pano:i18nRatchet`" }
        I18n.check("b · prose ratchet", off, "android/i18n/literal-counts.tsv must equal the sources' counts")
    }

    @Test fun `c every unkeyed tsv row is still in its file`() {
        val byFile = scans.associate { s -> s.rel to s.lits.map { it.cell }.toSet() }
        val off = unkeyed.filter { (f, t) -> byFile[f]?.contains(t) != true }.map { (f, t) -> "$f\t$t" }
        I18n.check("c · stale worklist row", off, "the text is no longer in its file: remove the row from android/i18n/unkeyed.tsv (the worklist must be pruned as it shrinks)")
    }

    @Test fun `d no unkeyed row for a ported file`() {
        val off = unkeyed.filter { it.first in ported }.map { "${it.first}\t${it.second}" } +
            ported.filter { p -> scans.none { it.rel == p } }.map { "$it: in ported.txt but not a phone source" }
        I18n.check("d · ported file", off, "a file in android/i18n/ported.txt is done: key the text, or take the file out of ported.txt")
    }

    private val FORMATS = listOf(Regex("""\bString\s*\.\s*format\s*\("""), Regex(""""\s*\.\s*format\s*\("""),
        Regex("""\bofPattern\s*\("""), Regex("""\bSimpleDateFormat\s*\("""))

    private fun argsAt(masked: String, open: Int): String {
        var d = 0; var i = open
        while (i < masked.length) { if (masked[i] == '(') d++ else if (masked[i] == ')') { d--; if (d == 0) break }; i++ }
        return masked.substring(open, minOf(i + 1, masked.length))
    }

    /**
     * A FIXED locale. `Locale.getDefault()`, `Locale("mr")` or a variable is the system language again,
     * which prints Devanagari digits on a phone whose system language is Marathi -- the hazard (e) exists for.
     */
    private val FIXED_LOCALE = Regex("""\bLocale\s*\.\s*(?:ROOT|US|ENGLISH)\b""")

    /** Every format call in [scans] that names no fixed Locale, as `File.kt: <its line>`. */
    private fun formatsWithoutLocale(scans: List<LiteralScan.FileScan>): Set<String> {
        val found = LinkedHashSet<String>()
        for (s in scans) for (rx in FORMATS) for (m in rx.findAll(s.masked)) {
            if (!FIXED_LOCALE.containsMatchIn(argsAt(s.masked, m.range.last))) found += "${s.name}: ${s.lineText(s.lineOf(m.range.first)).trim()}"
        }
        return found
    }

    @Test fun `e every format call names a Locale`() {
        // No pinned exceptions since slice 1c (27 Oct 2026): the 8 :app sites named Locale.ROOT, as the 7 :pano ones did in 1a.
        I18n.check("e · format without a Locale", formatsWithoutLocale(scans),
            "name a fixed Locale (Locale.ROOT): with none, or the default one, a phone whose SYSTEM language is Marathi prints Devanagari digits")
    }

    private val TX_REF = Regex("""(?<![\w.])tx\s*\.\s*([A-Za-z_]\w*)""")
    private val TX_STATIC_REF = Regex("""\bTx\s*\.\s*(?:EN|MR)\s*\.\s*([A-Za-z_]\w*)""")
    private val MASTERS_REF = Regex("""\bMasters\s*\.\s*([A-Za-z_]\w*)""")

    /** -1 for a property read; else the number of arguments in the call that follows [at]. */
    private fun arity(masked: String, at: Int): Int {
        var i = at; while (i < masked.length && masked[i].isWhitespace()) i++
        if (i >= masked.length || masked[i] != '(') return -1
        val a = argsAt(masked, i).removePrefix("(").removeSuffix(")")
        if (a.isBlank()) return 0
        var d = 0; var n = 1
        for (c in a) { if (c in "([{") d++ else if (c in ")]}") d-- else if (c == ',' && d == 0) n++ }
        return n
    }

    /** The top-level arguments of the call whose `(` is at [open] in [masked] (string bodies already blank). */
    private fun argList(masked: String, open: Int): List<String> {
        val a = argsAt(masked, open).removePrefix("(").removeSuffix(")")
        if (a.isBlank()) return emptyList()
        val out = ArrayList<String>(); var d = 0; var from = 0
        for ((i, c) in a.withIndex()) { if (c in "([{") d++ else if (c in ")]}") d-- else if (c == ',' && d == 0) { out += a.substring(from, i); from = i + 1 } }
        out += a.substring(from)
        return out
    }

    private val NAMED_ARG = Regex("""^\s*([A-Za-z_]\w*)\s*=(?!=)""")

    /**
     * Each template accessor's parameter names, in order, from the generated android/i18n/index.tsv
     * (check 6 keeps it current). They follow the ENGLISH placeholder order, so a prototype that rewords
     * its English can reorder them; a call that names its arguments survives that, a positional one
     * swaps two same-typed values and still compiles.
     */
    private val paramNames: Map<String, List<String>> by lazy {
        I18n.i18n.resolve(GenTx.INDEX_TSV).readLines().filter { !it.startsWith("#") && !it.startsWith("key\t") && it.isNotBlank() }
            .map { it.split('\t') }.filter { it.size >= 4 && it[2] == "template" && it[3] != "-" }
            .associate { c -> c[1] to c[3].split(" · ").map { it.substringBefore(':').trim() } }
    }

    /** Check (f) over [scans]: unresolved accessors, wrong arity, and positional or misnamed arguments. */
    private fun unresolved(scans: List<LiteralScan.FileScan>): List<String> {
        val tx = Tx::class.java.methods.groupBy { it.name }
        val ms = Masters::class.java.methods.groupBy { it.name }
        fun has(map: Map<String, List<java.lang.reflect.Method>>, name: String, n: Int): Boolean =
            if (n < 0) map["get" + name.replaceFirstChar { it.uppercase() }]?.any { it.parameterCount == 0 } == true
            else map[name]?.any { it.parameterCount == n } == true
        val off = ArrayList<String>()
        for (s in scans) {
            for (rx in listOf(TX_REF, TX_STATIC_REF)) for (m in rx.findAll(s.masked)) {
                val name = m.groupValues[1]; val n = arity(s.masked, m.range.last + 1)
                val at = "${s.name}:${s.lineOf(m.range.first)}"
                if (!has(tx, name, n)) { off += "$at: tx.$name${if (n < 0) "" else "/$n"} is not on Tx"; continue }
                if (n < 2) continue
                val want = paramNames[name] ?: continue
                var i = m.range.last + 1; while (i < s.masked.length && s.masked[i].isWhitespace()) i++
                val args = argList(s.masked, i).map { NAMED_ARG.find(it)?.groupValues?.get(1) }
                if (args.any { it == null }) off += "$at: tx.$name takes ${want.joinToString(", ")} -- name every argument (${want.joinToString(", ") { "$it = …" }}), or a reworded English silently swaps them"
                else args.filter { it !in want }.forEach { off += "$at: tx.$name has no parameter '$it' (it takes ${want.joinToString(", ")})" }
            }
            for (m in MASTERS_REF.findAll(s.masked)) {
                val name = m.groupValues[1]; val n = arity(s.masked, m.range.last + 1)
                if (!has(ms, name, n)) off += "${s.name}:${s.lineOf(m.range.first)}: Masters.$name${if (n < 0) "" else "/$n"} does not exist"
            }
        }
        return off
    }

    @Test fun `f every tx accessor the app calls exists with that many arguments`() {
        // :app and :camera: the modules this container cannot compile (:pano's own calls are compiled here)
        val off = unresolved(scans.filter { !it.rel.startsWith("pano/") })
        I18n.check("f · unresolved accessor", off, "the app calls an accessor the generated table does not have, or passes two or more without naming them: check the key in android/i18n/index.tsv")
    }

    // ---- (i) a member of a :pano object read from :app or :camera

    private val PKG = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE)
    /** A public top-level `object` (an `internal` one is not reachable from another module). */
    private val TOP_OBJECT = Regex("""^(?:public\s+)?object\s+([A-Z]\w*)""", RegexOption.MULTILINE)
    /** Any top-level declaration, by its name: one in :app or :camera shadows the :pano object of that name. */
    private val TOP_DECL = Regex("""^(?:(?:public|private|internal|data|sealed|enum|abstract|open|value|annotation|fun)\s+)*(?:class|object|interface|typealias)\s+([A-Z]\w*)""", RegexOption.MULTILINE)

    /** :pano's public top-level objects by simple name: what :app and :camera reach as `Name.member`. */
    private val panoObjects: Map<String, Class<*>> by lazy {
        val out = LinkedHashMap<String, Class<*>>()
        for (s in scans.filter { it.rel.startsWith("pano/") }) {
            val pkg = PKG.find(s.masked)?.groupValues?.get(1) ?: continue
            for (m in TOP_OBJECT.findAll(s.masked)) out[m.groupValues[1]] = Class.forName("$pkg.${m.groupValues[1]}")
        }
        out
    }

    /**
     * [name] is reachable on the object [cls]: a nested class or object (`SurveyPrep.Measure`), a function (an
     * `internal` one is mangled `name$module`), a property through its getter (`NAME` -> `getNAME`), or a `const val`
     * / `@JvmField` (a static field). Arity is not checked: the member's existence is what a rename or a delete breaks.
     */
    private fun hasMember(cls: Class<*>, name: String): Boolean {
        if (name[0].isUpperCase() && runCatching { Class.forName(cls.name + "$" + name) }.isSuccess) return true
        val getter = "get" + name.replaceFirstChar { it.uppercase() }
        if (cls.methods.any { it.name == name || it.name == getter || it.name.startsWith("$name$") }) return true
        return cls.fields.any { it.name == name }
    }

    /** Check (i) over [scans]: a `Name.member` read of one of [objects] that the object does not have. */
    private fun unresolvedObjectMembers(scans: List<LiteralScan.FileScan>, objects: Map<String, Class<*>>): List<String> {
        val own = scans.flatMap { s -> TOP_DECL.findAll(s.masked).map { it.groupValues[1] } }.toSet()
        val names = objects.keys - own
        if (names.isEmpty()) return emptyList()
        val rx = Regex("""(?<![\w.])(${names.joinToString("|")})\s*\.\s*([A-Za-z_]\w*)""")
        val off = ArrayList<String>()
        for (s in scans) for (m in rx.findAll(s.masked)) {
            val (obj, member) = m.destructured
            if (!hasMember(objects.getValue(obj), member)) off += "${s.name}:${s.lineOf(m.range.first)}: $obj.$member is not on the :pano object $obj"
        }
        return off
    }

    @Test fun `i every pano object member the app reads exists`() {
        // the other half of the compiler stand-in (f): a function, property, constant or nested class of a :pano object
        // read as `Name.member` from :app or :camera. M3a renamed SurveyPrepEdit.LINES_REFUSAL to linesRefusal(tx) and
        // converted one of its two readers; (f) scans `tx.<name>` alone, so :pano:test stayed green and CI's kotlinc
        // would have been the first to say `Unresolved reference: LINES_REFUSAL` (M3a review)
        val off = unresolvedObjectMembers(scans.filter { !it.rel.startsWith("pano/") }, panoObjects)
        I18n.check("i · unresolved object member", off, "the app reads a member a :pano object does not have: rename the read with the object (only CI compiles :app and :camera)")
    }

    @Test fun `i sees a deleted constant, and not a function, property, constant or nested class that exists`() {
        val src = "fun f() {\n" +
            "    say(SurveyPrepEdit.LINES_REFUSAL)\n" +            // the const val M3a deleted
            "    say(SurveyPrepEdit.linesRefusal(tx))\n" +         // a function
            "    val n = SurveyPrep.NAME.getValue(s)\n" +           // a property, through its getter
            "    val k = SurveyPrepEdit.KIND_TAPE\n" +              // a constant, upper-case like a class
            "    val m = SurveyPrep.Measure(\"w\", \"width\")\n" + // a nested class
            "    val r = StepRoute.TAB_ROWS.flatten()\n" +          // a property with an upper-case name
            "    // SurveyPrepEdit.NOT_A_MEMBER in a comment is not a read\n" +
            "    val t = \"SurveyPrepEdit.NOT_A_MEMBER in a string is not a read\"\n" +
            "    val own = SurveyPrepDraw.NOT_A_MEMBER\n" +         // not a :pano object
            "}\n"
        val off = unresolvedObjectMembers(listOf(LiteralScan.scanText("app/src/main/kotlin/X.kt", src)), panoObjects)
        assertEquals(listOf("X.kt:2: SurveyPrepEdit.LINES_REFUSAL is not on the :pano object SurveyPrepEdit"), off)
        // a declaration of the same name in :app shadows the :pano object, so its members are not checked
        val shadow = "object SurveyPrepEdit { val MINE = 1 }\nfun g() = SurveyPrepEdit.MINE\n"
        assertEquals(emptyList(), unresolvedObjectMembers(listOf(LiteralScan.scanText("app/src/main/kotlin/Y.kt", shadow)), panoObjects))
    }

    private val SYSTEM_LANGUAGE = Regex("""\bLocale\s*\.\s*getDefault\b|\bLocaleList\b|\.\s*locales\b|\bconfiguration\s*\.\s*locale\b""")

    private fun systemLanguageReads(scans: List<LiteralScan.FileScan>): List<String> = scans.flatMap { s ->
        SYSTEM_LANGUAGE.findAll(s.masked).map { "${s.name}:${s.lineOf(it.range.first)}: ${it.value}" }.toList()
    }

    @Test fun `g no source reads the system language`() {
        // every module: :pano's redraw keys and exports would change bytes with the system language too
        I18n.check("g · system language", systemLanguageReads(scans), "the language comes from the भाषा · Language row alone; English on first run whatever the phone's language")
    }

    private val TX_MENTION = Regex("""(?<![\w.])tx\b|\bTx\b|\bLocalTx\b|\bMasters\b|\bMsg\b""")

    /** The one mention check (h) permits in FaceWriter.kt and SurveyExport.kt: the LITERAL `Tx.EN`, never read
     *  further (`Tx.EN.x` would compose text here) -- the painters take a tx since M3b and the drawing takes one
     *  since M3c, and the sheet renders pin English by passing it (decision 4's enforcement stays (h)'s: the
     *  export sees no LIVE language, only the pinned English table). */
    private fun isPinnedEnglish(masked: String, m: MatchResult): Boolean {
        if (m.value != "Tx") return false
        val tail = masked.substring(m.range.last + 1)
        return Regex("""^\s*\.\s*EN\b(?!\s*\.)""").containsMatchIn(tail)
    }

    /** The files (h) scans whole, by their full rel: the gate is the rel itself, so a pano region scans too --
     *  keyed on s.name and gated on app/, the SurveyExport region would have been built and then silently never
     *  scanned, a check green while scanning nothing (the M3c critic's finding 1). */
    private val H_WHOLE = listOf(
        "app/src/main/kotlin/com/malletcrafts/interiorestimate/PlanExport.kt",
        "app/src/main/kotlin/com/malletcrafts/interiorestimate/FaceWriter.kt",
        // M3c: the Sheet IS the file (its preview paints through the PDF's own painter), so the whole of
        // SurveyExport.kt -- SvgWriter included -- is scanned, with FaceWriter's exemption: the literal Tx.EN
        // plus the import that names it
        "pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/SurveyExport.kt",
    )
    private val H_UPLOAD = "app/src/main/kotlin/com/malletcrafts/interiorestimate/AsIsUpload.kt"
    /** The files whose sheet renders may pass the painter or the drawing the literal `Tx.EN`. */
    private val H_PINNED = setOf("FaceWriter.kt", "SurveyExport.kt")

    /** Check (h) over [scans]: every Tx mention in an export or upload builder, minus the pinned-English exemption. */
    private fun exportBuilderMentions(scans: List<LiteralScan.FileScan>): List<String> {
        val off = ArrayList<String>()
        for (s in scans) {
            val region: IntRange? = when (s.rel) {
                in H_WHOLE -> 0 until s.masked.length
                H_UPLOAD -> {
                    // the worker that builds and sends the record: no screen, so never a language
                    val at = Regex("""\bfun\s+doWork\b""").find(s.masked)?.range?.first
                    if (at == null) { off += "AsIsUpload.kt: no fun doWork (update this check with the body builder's new name)"; null }
                    else {
                        val open = s.masked.indexOf('{', at); var d = 0; var i = open
                        while (i < s.masked.length) { if (s.masked[i] == '{') d++ else if (s.masked[i] == '}') { d--; if (d == 0) break }; i++ }
                        open..i
                    }
                }
                else -> null
            }
            if (region != null) {
                TX_MENTION.findAll(s.masked.substring(region.first, region.last + 1)).forEach {
                    // M3b/M3c: FaceWriter's survey sheets and SurveyExport's sheet() pass the literal `Tx.EN`
                    // (never `tx`, never LocalTx, never Tx.MR, never a read like Tx.EN.x) -- the one exemption,
                    // pinned below; the import that names it is the same exemption
                    val line = s.lineText(s.lineOf(region.first + it.range.first)).trim()
                    val pinned = s.name in H_PINNED &&
                        (isPinnedEnglish(s.masked.substring(region.first, region.last + 1), it) ||
                         line == "import com.malletcrafts.interiorestimate.pano.i18n.Tx")
                    if (!pinned) off += "${s.name}:${s.lineOf(region.first + it.range.first)}: ${it.value}"
                }
            }
        }
        return off
    }

    @Test fun `h the export and upload builders never see Tx`() {
        val off = ArrayList<String>()
        off += exportBuilderMentions(scans)
        (H_WHOLE + H_UPLOAD).filter { rel -> scans.none { it.rel == rel } }
            .forEach { off += "${it.substringAfterLast('/')}: not found (update this check)" }
        I18n.check("h · Tx in an export or upload builder", off, "the PDF, the images, the saved sheets and the upload body stay English (decision 4 of 21 Oct): they take no Tx -- FaceWriter and SurveyExport may pass only the literal Tx.EN (M3b, M3c)")
    }

    @Test fun `h fires on a Tx mention at SurveyExport's pano path, and passes the pinned Tx EN with its import`() {
        // the M3c critic's finding 1: a region built but never scanned is this project's recorded worst failure
        // shape, so the pano region's firing is proven on a planted mention, not assumed
        val rel = "pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/SurveyExport.kt"
        val bad = "import com.malletcrafts.interiorestimate.pano.i18n.Tx\n" +
            "fun sheet(tx: Tx) = SurveyPlan.drawing(tx, \"floor\")\n"
        assertEquals(listOf("SurveyExport.kt:2: tx", "SurveyExport.kt:2: Tx", "SurveyExport.kt:2: tx"),
            exportBuilderMentions(listOf(LiteralScan.scanText(rel, bad))))
        val good = "import com.malletcrafts.interiorestimate.pano.i18n.Tx\n" +
            "fun sheet() = SurveyPlan.drawing(Tx.EN, \"floor\")\n"
        assertEquals(emptyList(), exportBuilderMentions(listOf(LiteralScan.scanText(rel, good))))
        // a read THROUGH the pinned table (Tx.EN.x) composes text in the export and is refused
        val read = "fun sheet() = title(Tx.EN.common_export)\n"
        assertEquals(listOf("SurveyExport.kt:1: Tx"), exportBuilderMentions(listOf(LiteralScan.scanText(rel, read))))
    }

    @Test fun `h the sheet render and the face-prep export are pinned English`() {
        // THE (h) COMPANION (port 61c slice M3b; the M3b critic's findings 2 and 3). The painters take a tx now, so
        // decision 4 is enforced by WHAT IS PASSED: FaceWriter's survey sheet render and FacePrepScreen.exportAll's
        // ImageMeter render each pass the literal `Tx.EN` -- and exportAll (FacePrepDraw's second caller; never
        // FaceWriter, which draws through SurveyPrepDraw alone) sees no other Tx mention, so M8's flip of the SCREEN
        // Canvas call cannot silently turn the exported photos Marathi.
        val off = ArrayList<String>()
        val fw = scans.firstOrNull { it.name == "FaceWriter.kt" }
        if (fw == null) off += "FaceWriter.kt: not found (update this check)"
        else if (!Regex("""SurveyPrepDraw\s*\.\s*drawMarks\(\s*Canvas\(flat\),\s*Tx\.EN,""").containsMatchIn(fw.masked))
            off += "FaceWriter.kt: the survey sheet render no longer passes Tx.EN as drawMarks' language"
        // M3c: the A4 sheet's drawing is pinned English the same way -- sheet() passes the literal Tx.EN
        val se = scans.firstOrNull { it.name == "SurveyExport.kt" }
        if (se == null) off += "SurveyExport.kt: not found (update this check)"
        else if (!Regex("""SurveyPlan\s*\.\s*drawing\(\s*Tx\.EN,""").containsMatchIn(se.masked))
            off += "SurveyExport.kt: sheet() no longer passes Tx.EN as the drawing's language"
        val fps = scans.firstOrNull { it.name == "FacePrepScreen.kt" }
        if (fps == null) off += "FacePrepScreen.kt: not found (update this check)"
        else {
            val at = Regex("""\bfun\s+exportAll\b""").find(fps.masked)?.range?.first
            if (at == null) off += "FacePrepScreen.kt: no fun exportAll (update this check with the ImageMeter export's new name)"
            else {
                val open = fps.masked.indexOf('{', at); var d = 0; var i = open
                while (i < fps.masked.length) { if (fps.masked[i] == '{') d++ else if (fps.masked[i] == '}') { d--; if (d == 0) break }; i++ }
                val body = fps.masked.substring(open, minOf(i + 1, fps.masked.length))
                TX_MENTION.findAll(body).forEach {
                    if (!isPinnedEnglish(body, it)) off += "FacePrepScreen.kt:${fps.lineOf(open + it.range.first)}: ${it.value} in exportAll"
                }
                if (!Regex("""FacePrepDraw\s*\.\s*draw\(\s*c,\s*Tx\.EN,""").containsMatchIn(body))
                    off += "FacePrepScreen.kt: exportAll no longer passes Tx.EN as FacePrepDraw.draw's language"
            }
        }
        I18n.check("h · the exports' language is pinned", off,
            "the ImageMeter photos and the survey sheets stay English (decision 4): their one painter argument is the literal Tx.EN")
    }

    @Test fun `the annotate editor's marks are licensed by its gate`() {
        // Port 61c slice M8: the held annotator's ~35 texts are marked `// text:value dormant: behind
        // ANNOTATION_ENABLED = false`, because a text NO PERSON CAN SEE is what every existing mark has in
        // common — and Claude's Marathi there could never meet its only review, Amit's phone (decision 6).
        // The licence can lapse (someone flips the gate), so it is a TEST, not a comment: the day the editor
        // returns the suite goes red NAMING THE RECIPE instead of ~35 marked English strings silently
        // reaching the screen. A hazard written down in a mark is not a hazard handled.
        val s = scans.firstOrNull { it.name == "AnnotateScreen.kt" }
        val off = when {
            s == null -> listOf("AnnotateScreen.kt: not found (update this check)")
            !Regex("""\bprivate\s+const\s+val\s+ANNOTATION_ENABLED\s*=\s*false\b""").containsMatchIn(s.masked) ->
                listOf("AnnotateScreen.kt: ANNOTATION_ENABLED is not `private const val ANNOTATION_ENABLED = false` any more — " +
                    "the dormant `// text:value` marks were licensed by that gate: key the editor's rows through the " +
                    "then-current card and remove the dormant marks before the switch flips")
            else -> emptyList()
        }
        I18n.check("the annotate editor's gate", off,
            "the held annotator's English is licensed by ANNOTATION_ENABLED = false (M8): key its rows through the then-current card when the editor returns")
    }

    @Test fun `the language row is shown while the screens are translated alongside`() {
        // Amit, 28 Oct 2026, ~13:10 IST: "Show it now, translate alongside" -- decision 2 of 27 Oct
        // (hidden until every screen is translated) is reversed. The row ships now; a screen not keyed
        // yet stays English under मराठी until its slice. What stays is the worklist: every text still
        // English is a row of unkeyed.tsv (checks a-h and the ratchet keep it complete and shrinking).
        val visible: Boolean = LangRow.VISIBLE
        val off = if (!visible) listOf("LangRow.VISIBLE is false: the भाषा · Language row is hidden again") else emptyList()
        I18n.check("the language row", off, "the भाषा · Language row is shown (Amit, 28 Oct 2026: show it now, translate alongside)")
    }

    /** The row's gate, straight in front of the call: `if (LangRow.VISIBLE) DrawerLine(`. */
    private val ROW_GATE = Regex("""\bif\s*\(\s*LangRow\s*\.\s*VISIBLE\s*\)\s*DrawerLine\s*\(""")
    /** An argument that opens with the gate: `if (LangRow.VISIBLE)`. */
    private val ARG_GATE = Regex("""^\s*if\s*\(\s*LangRow\s*\.\s*VISIBLE\s*\)""")
    /** `else null`, and nothing that would make the else-branch more than `null` (`?: x`, `.let`, `as`, `==`…). */
    private val ELSE_NULL = Regex("""^\s*else\s+null\b(?!\s*(?:[.?!=<>+\-*/%&|]|as\b|in\b|is\b))""")
    private val TX_OF = Regex("""\bTx\s*\.\s*of\s*\(""")
    private val TX_MR = Regex("""\bTx\s*\.\s*MR\b""")

    /**
     * [lit] is the whole first argument of the DrawerLine whose `(` is at [open] in [masked], and the
     * call is followed by `else null`: with [ROW_GATE] in front, `if (LangRow.VISIBLE) DrawerLine(lit, …) else null`.
     */
    private fun gatedRow(masked: String, open: Int, lit: LiteralScan.Lit): Boolean {
        val first = argList(masked, open).firstOrNull() ?: return false
        if (open + 1 + (first.length - first.trimStart().length) != lit.start || first.trim().length != lit.end - lit.start) return false
        val close = open + argsAt(masked, open).length - 1
        return masked.getOrNull(close) == ')' && ELSE_NULL.containsMatchIn(masked.substring(close + 1))
    }

    /** [arg] is exactly `if (LangRow.VISIBLE) <expr> else null`: one `else` at the gate's own depth, and its branch is `null`. */
    private fun gatedElseNull(arg: String): Boolean {
        val g = ARG_GATE.find(arg) ?: return false
        val rest = arg.substring(g.range.last + 1)
        fun word(c: Char?) = c != null && (c.isLetterOrDigit() || c == '_')
        var d = 0; val elses = ArrayList<Int>()
        for ((i, c) in rest.withIndex()) {
            if (c in "([{") d++ else if (c in ")]}") d--
            else if (d == 0 && rest.startsWith("else", i) && !word(rest.getOrNull(i - 1)) && !word(rest.getOrNull(i + 4))) elses += i
        }
        return elses.size == 1 && rest.substring(0, elses[0]).isNotBlank() && rest.substring(elses[0] + 4).trim() == "null"
    }

    /**
     * The constant alone proves nothing if the row or the provider does not read it (slice 1b), and a
     * mention is not a gate: the भाषा · Language row must be drawn as exactly
     * `if (LangRow.VISIBLE) DrawerLine("भाषा · Language", …) else null`, every `Tx.of(` in :app and
     * :camera must be exactly `Tx.of(if (LangRow.VISIBLE) <the stored choice> else null)`, and nothing
     * there may name Tx.MR directly. So an inverted gate (`if (LangRow.VISIBLE) null else …`), a negated
     * one or a gate in a comment is refused. One switch for the row and for picking मराठी (decision 2),
     * never a second.
     */
    private fun ungated(scans: List<LiteralScan.FileScan>): List<String> {
        val off = ArrayList<String>()
        var rows = 0
        for (s in scans) {
            val calls = ROW_GATE.findAll(s.masked).map { it.range.last }.toList()
            for (l in s.lits) if (l.text == "भाषा · Language") {
                rows++
                if (calls.none { gatedRow(s.masked, it, l) }) off += "${s.name}:${l.line}: the भाषा · Language row is drawn other than as `if (LangRow.VISIBLE) DrawerLine(\"भाषा · Language\", …) else null`"
            }
            for (m in TX_OF.findAll(s.masked)) {
                val args = argList(s.masked, m.range.last)
                if (args.size != 1 || !gatedElseNull(args[0])) off += "${s.name}:${s.lineOf(m.range.first)}: Tx.of(…) picks the language other than as `Tx.of(if (LangRow.VISIBLE) <the stored choice> else null)`"
            }
            for (m in TX_MR.findAll(s.masked)) off += "${s.name}:${s.lineOf(m.range.first)}: Tx.MR picks Marathi without the row"
        }
        if (rows != 1) off += "$rows भाषा · Language rows in :app and :camera; the drawer has exactly one, straight after Units"
        return off
    }

    @Test fun `the language row and the provider read LangRow VISIBLE`() {
        I18n.check("the language switch", ungated(scans.filter { !it.rel.startsWith("pano/") }),
            "the भाषा · Language row and the ability to pick मराठी switch on together, by LangRow.VISIBLE alone (Amit, 27 Oct 2026, decision 2)")
    }

    @Test fun `the language switch check refuses an ungated or inverted row, provider or Tx MR`() {
        val good = "val tx = remember { Tx.of(if (LangRow.VISIBLE) p.lang else null) }\n" +
            "val l = if (LangRow.VISIBLE) DrawerLine(\"भाषा · Language\", value = \"English\") else null // text:value\n"
        assertEquals(emptyList(), ungated(listOf(LiteralScan.scanText("app/src/main/kotlin/G.kt", good))))
        val bad = "val tx = remember { Tx.of(p.lang) } // LangRow.VISIBLE\n" +
            "val l = DrawerLine(\"भाषा · Language\", value = \"English\") // if (LangRow.VISIBLE)\n" +
            "val m = Tx.MR.app_sync\n" +
            // inverted: each names the gate where the old check looked for it, and turns decision 2 off
            "val t2 = remember { Tx.of(if (LangRow.VISIBLE) null else AppPrefs.read(prefs).lang) }\n" +
            "val l2 = if (LangRow.VISIBLE) null else DrawerLine(\"भाषा · Language\", value = \"English\") // text:value\n"
        val off = ungated(listOf(LiteralScan.scanText("app/src/main/kotlin/B.kt", bad)))
        assertEquals(6, off.size, off.joinToString("\n"))
        assertTrue(off[0].startsWith("B.kt:2: the भाषा · Language row is drawn other than as"), off[0])
        assertTrue(off[1].startsWith("B.kt:5: the भाषा · Language row is drawn other than as"), off[1])
        assertTrue(off[2].startsWith("B.kt:1: Tx.of(…) picks the language other than as"), off[2])
        assertTrue(off[3].startsWith("B.kt:4: Tx.of(…) picks the language other than as"), off[3])
        assertTrue(off[4].startsWith("B.kt:3: Tx.MR picks Marathi"), off[4])
        assertTrue(off[5].startsWith("2 भाषा · Language rows"), off[5])
    }

    @Test fun `e and g refuse the default locale, in pano as in the app`() {
        val s = LiteralScan.scanText("pano/src/main/kotlin/X.kt",
            "fun f(x: Double) = \"%.1f\".format(java.util.Locale.getDefault(), x)\nfun g(x: Double) = \"%.1f\".format(Locale.ROOT, x)\n")
        assertEquals(setOf("X.kt: fun f(x: Double) = \"%.1f\".format(java.util.Locale.getDefault(), x)"), formatsWithoutLocale(listOf(s)))
        assertEquals(listOf("X.kt:1: Locale.getDefault"), systemLanguageReads(listOf(s)))
    }

    @Test fun `f requires named arguments for an accessor with two or more parameters`() {
        val src = "fun f() {\n" +
            "    a = tx.app_x_x_rooms(3, 12)\n" +
            "    b = tx.app_x_x_rooms(size = 3, length = 12)\n" +
            "    c = tx.app_x_x_rooms(length = 12, size = 3)\n" +
            "    d = tx.app_x_x_rooms(size = 3, total = 12)\n" +
            "    e = Tx.EN.app_x_x_rooms(size = f(1, 2), length = 12)\n" +
            "    g = tx.pl_room(3)\n" +
            "}\n"
        val off = unresolved(listOf(LiteralScan.scanText("app/src/main/kotlin/X.kt", src)))
        assertEquals(2, off.size, off.joinToString("\n"))
        assertTrue(off[0].startsWith("X.kt:2: tx.app_x_x_rooms takes size, length -- name every argument"), off[0])
        assertTrue(off[1].startsWith("X.kt:5: tx.app_x_x_rooms has no parameter 'total'"), off[1])
    }

    @Test fun `b counts neither a marked nor a listed literal`() {
        val src = "val LocalTx = staticCompositionLocalOf<Tx> { error(\"LocalTx not provided\") } // text:value developer error, never shown\n" +
            "fun refuse() = \"Meter is busy\"\n"
        val s = LiteralScan.scanText("app/src/main/kotlin/Lang.kt", src)
        val (marked, plain) = s.lits
        assertEquals(listOf(false, true), listOf(marked, plain).map { LiteralScan.counted(s, it, emptySet()) })
        assertEquals(false, LiteralScan.counted(s, plain, setOf("app/src/main/kotlin/Lang.kt" to "Meter is busy")))
    }

    /** `lastResult = {`, `busy = {` or `note = {` not followed by a named parameter and `->`. */
    private val MSG_IMPLICIT = Regex("""\b(?:lastResult|busy|note)\s*=\s*\{(?!\s*[A-Za-z_]\w*\s*(?::\s*Tx\s*)?->)""")

    private fun implicitMsgParams(scans: List<LiteralScan.FileScan>): List<String> = scans.flatMap { s ->
        MSG_IMPLICIT.findAll(s.masked).map { "${s.name}:${s.lineOf(it.range.first)}: ${s.lineText(s.lineOf(it.range.first)).trim()}" }.toList()
    }

    @Test fun `a Msg lambda names its parameter`() {
        // slice 1c: with an implicit `it`, an `it.message` inside a `lastResult = { … }` means the Tx, a
        // break only :app's compiler (CI) would find; `{ tx -> … }` or `{ _ -> … }` keeps `it` the enclosing lambda's
        I18n.check("a Msg lambda's parameter", implicitMsgParams(scans),
            "name the parameter of a lambda assigned to lastResult, busy or note: `{ tx -> … }`, or `{ _ -> … }` for a text still English")
        val src = "fun f() {\n    lastResult = { _ -> \"a\" }\n    busy = { tx -> tx.app_sync }\n    note = { tx: Tx -> \"b\" }\n" +
            "    lastResult = { \"Could not: ${'$'}{it.message}\" }\n    busy = {\n        \"c\" }\n}\n"
        val off = implicitMsgParams(listOf(LiteralScan.scanText("app/src/main/kotlin/M.kt", src)))
        assertEquals(listOf(5, 6), off.map { it.substringAfter("M.kt:").substringBefore(':').toInt() }, off.joinToString("\n"))
    }

    /** A lambda parameter typed `Tx`: `{ t: Tx ->`. */
    private val TX_PARAM = Regex("""\{\s*([A-Za-z_]\w*)\s*:\s*Tx\s*->""")

    private fun txParamsNotTx(scans: List<LiteralScan.FileScan>): List<String> = scans.flatMap { s ->
        TX_PARAM.findAll(s.masked).filter { it.groupValues[1] != "tx" && it.groupValues[1] != "_" }
            .map { "${s.name}:${s.lineOf(it.range.first)}: `{ ${it.groupValues[1]}: Tx -> … }` reads the table through a name (f) does not scan" }.toList()
    }

    @Test fun `a Tx lambda names its parameter tx, so f resolves its reads`() {
        // M3a review: three `{ t: Tx -> t.sp_msg_saved … }` in SurveyPrepScreen.kt read five accessors (f) never resolved,
        // since (f) scans `tx.<name>` alone (a `t.` is any value). `tx` shadows the composable's own: a warning, never an error
        I18n.check("a Tx lambda's parameter", txParamsNotTx(scans.filter { !it.rel.startsWith("pano/") }),
            "name a lambda's Tx parameter tx (`{ tx: Tx -> tx.app_sync }`), or _ when it is unused, so check (f) resolves what it reads")
        val src = "fun f() {\n    savedMsg = if (ok) { t: Tx -> t.sp_msg_saved } else { tx: Tx -> tx.sp_msg_not_saved_did_not }\n" +
            "    note = { _: Tx -> \"b\" }\n    other = {t :Tx-> t.app_sync }\n    busy = { tx -> tx.app_sync }\n}\n"
        val off = txParamsNotTx(listOf(LiteralScan.scanText("app/src/main/kotlin/M.kt", src)))
        assertEquals(listOf(2, 4), off.map { it.substringAfter("M.kt:").substringBefore(':').toInt() }, off.joinToString("\n"))
    }

    /** A loop over `SurveyPrepFile.Decoded.problems`, a `List<Msg>`: `for (<v> in <…>.problems)`. */
    private val PROBLEMS_LOOP = Regex("""\bfor\s*\(\s*([A-Za-z_]\w*)\s+in\s+(?:[\w.]+\.)?problems\s*\)""")

    /** The loop statement after [after] in [masked]: a balanced `{ … }` block, or the rest of the line. */
    private fun loopBody(masked: String, after: Int): IntRange {
        var i = after
        while (i < masked.length && masked[i].isWhitespace()) i++
        if (masked.getOrNull(i) == '{') {
            var d = 0; var j = i
            while (j < masked.length) { if (masked[j] == '{') d++ else if (masked[j] == '}') { d--; if (d == 0) break }; j++ }
            return i..minOf(j, masked.length - 1)
        }
        val end = masked.indexOf('\n', i).let { if (it < 0) masked.length else it }
        return i until end
    }

    /**
     * Check over [scans]: a Msg iterated from `.problems` that a string template takes BARE. `"$id: $p"`
     * compiles on a lambda -- a template takes Any -- and prints the lambda's toString, not the note, so
     * the loop variable must be CALLED (`${p(Tx.EN)}`), never interpolated as `$p` or `${p}`.
     * String bodies are blanked in [LiteralScan.FileScan.masked] but template code is kept, so the scan sees it.
     */
    private fun bareMsgInterpolations(scans: List<LiteralScan.FileScan>): List<String> {
        val off = ArrayList<String>()
        for (s in scans) for (m in PROBLEMS_LOOP.findAll(s.masked)) {
            val v = Regex.escape(m.groupValues[1])
            val body = loopBody(s.masked, m.range.last + 1)
            for (b in Regex("""\$(?:$v\b|\{\s*$v\s*\})""").findAll(s.masked.substring(body.first, body.last + 1)))
                off += "${s.name}:${s.lineOf(body.first + b.range.first)}: a Msg from .problems interpolated bare (${b.value})"
        }
        return off
    }

    @Test fun `a Msg from problems is rendered, never interpolated bare`() {
        // The M3b review (its one confirmed major): Decoded.problems became List<Msg> and SurveyPrepStore.open's
        // Log.w still said "$id: $p" -- a template takes Any, so CI compiled it and the logcat line printed the
        // lambda's toString instead of "Front wall: its floor line was unreadable, placed again". That Log.w is
        // the ONLY full record of what a salvage dropped (the screen's note shows problems.first() plus a count),
        // so the record died silently -- the disabled-path version-stamp fault by a new route: a TYPE change
        // consumed through Any, which (i) cannot see (`problems` kept its name) and only CI's kotlinc compiles,
        // and kotlinc compiles WITHOUT complaint. The idiom: render with the pinned English table, `p(Tx.EN)`
        // -- a logcat line is a diagnostic other parties read (decision 4's register, ReadingsSync's stored note).
        I18n.check("a bare Msg in a template", bareMsgInterpolations(scans),
            "a template on a Msg prints the lambda's toString: render the problem -- `\${p(Tx.EN)}` in a log -- never `\$p` or `\${p}`")
    }

    @Test fun `the bare-Msg check sees the review's line and passes the fix`() {
        // the bad fixture's line 2 is SurveyPrepStore.kt:82 as M3b shipped it, byte for byte
        val bad = "fun f() {\n" +
            "    for (p in o.decoded.problems) Log.w(TAG, \"\$id: \$p\")\n" +
            "    for (q in d.problems) {\n        Log.w(TAG, \"\$id: \${q}\")\n    }\n" +
            "}\n"
        val off = bareMsgInterpolations(listOf(LiteralScan.scanText("app/src/main/kotlin/X.kt", bad)))
        assertEquals(listOf(2, 4), off.map { it.substringAfter("X.kt:").substringBefore(':').toInt() }, off.joinToString("\n"))
        // rendered problems pass; a List<String> loop (pointLog) is out of scope, its $p is fine
        val good = "fun f() {\n" +
            "    for (p in o.decoded.problems) Log.w(TAG, \"\$id: \${p(Tx.EN)}\")\n" +
            "    for (p in o.decoded.pointLog) Log.i(TAG, \"\$id: \$p\")\n" +
            "}\n"
        assertEquals(emptyList(), bareMsgInterpolations(listOf(LiteralScan.scanText("app/src/main/kotlin/Y.kt", good))))
    }

    @Test fun `a Msg lambda assigned to lastResult, busy or note is a UI position`() {
        // slice 1c: the transient messages hold a Msg, so the text a person sees is the lambda's value
        val src = """
            fun f() {
                lastResult = { _ -> "Could not rename: ${'$'}{it.message}" }
                busy = { tx -> if (photo) tx.app_filing_photo else "Splitting the 360" }
                onFailure { note = { _ -> "Not saved" +
                    " here" } }
                lastResult = { _ -> info.optString("version_name") }
                val m: Msg = { _ -> "a Msg kept for later" }
                other = { _ -> "another lambda" }
            }
        """.trimIndent()
        val s = LiteralScan.scanText("fixture/M.kt", src)
        assertEquals(listOf("Could not rename: ", "Splitting the 360", "Not saved", " here"), s.lits.filter { it.ui }.map { it.text })
        assertEquals(listOf("version_name", "a Msg kept for later", "another lambda"), s.lits.filter { !it.ui }.map { it.text })
    }

    @Test fun `the tokenizer sees what the readers' tokenizer saw`() {
        val src = """
            fun f() {
                Text("Shown")
                val id = "not_shown"
                lastResult = if (ok) "Saved" else "Not saved"
                val s = "plain prose here"
                // Text("a comment")
                /* Text("block /* nested */ comment") */
                say("Hello ${'$'}{name.ifBlank { "nobody" }} there")
                when (x) { "Waiting" -> Text("Waits") }
                DrawerLine("Units", value = if (imp) "mm · ft-in" else "mm")
                val c = '"'
                Text(text = "named")
                foo(title = "Title arg")
                val t = "a\tb"
            }
        """.trimIndent()
        val s = LiteralScan.scanText("fixture/F.kt", src)
        val ui = s.lits.filter { it.ui }.map { it.text }
        val not = s.lits.filter { !it.ui }.map { it.text }
        assertEquals(listOf("Shown", "Saved", "Not saved", "Hello  there", "nobody", "Waits", "Units", "mm · ft-in", "mm", "named", "Title arg"), ui)
        assertEquals(listOf("not_shown", "plain prose here", "Waiting", "a\tb"), not)
        assertEquals("a\\tb", s.lits.last().cell)
        assertEquals(listOf(false, true), not.map { s.lits.first { l -> l.text == it }.let(LiteralScan::prose) }.take(2))
    }
}
