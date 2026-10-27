package com.malletcrafts.interiorestimate.pano.i18n

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * EVERY TEXT A PERSON SEES IS KEYED, LISTED OR MARKED (design-marathi §3.3, I18nLiteralTest a–h).
 * Scans `android/app/src/main/kotlin` and `android/pano/src/main/kotlin` (the generated table
 * excepted) with [LiteralScan]. This is also the local stand-in for :app's compiler on the
 * commonest break, an unresolved name (f): two of those broke `main` on 21 Sep.
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
        val off = ArrayList<String>()
        for (s in scans) {
            val prose = s.lits.filter { !it.ui && LiteralScan.prose(it) }
            val want = listed[s.rel] ?: 0
            if (prose.size > want) off += "${s.rel}: ${prose.size} prose literals, ${want} listed — a new one: key it, list it in unkeyed.tsv, or mark it `// text:value`. " +
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

    /** The :app format calls that name no Locale today; slice 1c gives each Locale.ROOT and empties this list. */
    private val APP_FORMATS_PINNED = setOf(
        "AsIsUpload.kt: .toLocalTime().format(DateTimeFormatter.ofPattern(\"HH:mm\"))",
        "DistoClient.kt: event(\"distance [\${Disto.hex(bytes)}] = %.4f m\".format(m))",
        "MainActivity.kt: localSize = keptFile?.let { \"%.1f MB\".format(it.length() / 1024.0 / 1024.0) } ?: \"\")",
        "MainActivity.kt: else -> String.format(\"%.1f MB\", bytes / 1024.0 / 1024.0)",
        "ReadingsSync.kt: fun now(): String = LocalTime.now().format(DateTimeFormatter.ofPattern(\"HH:mm\"))",
        "S5Session.kt: return \"S5-\" + d.joinToString(\"\") { \"%02x\".format(it) }.take(10)",
        "SurveyPrepScreen.kt: savedMsg = if (ok) \"Saved ✓ \" + LocalTime.now().format(DateTimeFormatter.ofPattern(\"HH:mm\")) else \"Not saved: did not read back\"",
        "ZoomableImage.kt: if (view.zoomed) \"%.1f×  ·  double-tap to fit\".format(view.scale)",
    )

    private val FORMATS = listOf(Regex("""\bString\s*\.\s*format\s*\("""), Regex(""""\s*\.\s*format\s*\("""),
        Regex("""\bofPattern\s*\("""), Regex("""\bSimpleDateFormat\s*\("""))

    private fun argsAt(masked: String, open: Int): String {
        var d = 0; var i = open
        while (i < masked.length) { if (masked[i] == '(') d++ else if (masked[i] == ')') { d--; if (d == 0) break }; i++ }
        return masked.substring(open, minOf(i + 1, masked.length))
    }

    @Test fun `e every format call names a Locale`() {
        val found = LinkedHashSet<String>()
        for (s in scans) for (rx in FORMATS) for (m in rx.findAll(s.masked)) {
            if ("Locale" !in argsAt(s.masked, m.range.last)) found += "${s.name}: ${s.lineText(s.lineOf(m.range.first)).trim()}"
        }
        val off = found.filter { it !in APP_FORMATS_PINNED } +
            (APP_FORMATS_PINNED - found).map { "$it: pinned but gone — remove it from APP_FORMATS_PINNED" }
        I18n.check("e · format without a Locale", off,
            "name a Locale (Locale.ROOT): with none, a phone whose SYSTEM language is Marathi prints Devanagari digits")
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

    @Test fun `f every tx accessor the app calls exists with that many arguments`() {
        val tx = Tx::class.java.methods.groupBy { it.name }
        val ms = Masters::class.java.methods.groupBy { it.name }
        fun has(map: Map<String, List<java.lang.reflect.Method>>, name: String, n: Int): Boolean =
            if (n < 0) map["get" + name.replaceFirstChar { it.uppercase() }]?.any { it.parameterCount == 0 } == true
            else map[name]?.any { it.parameterCount == n } == true
        val off = ArrayList<String>()
        for (s in scans.filter { it.rel.startsWith("app/") }) {
            for (rx in listOf(TX_REF, TX_STATIC_REF)) for (m in rx.findAll(s.masked)) {
                val name = m.groupValues[1]; val n = arity(s.masked, m.range.last + 1)
                if (!has(tx, name, n)) off += "${s.name}:${s.lineOf(m.range.first)}: tx.$name${if (n < 0) "" else "/$n"} is not on Tx"
            }
            for (m in MASTERS_REF.findAll(s.masked)) {
                val name = m.groupValues[1]; val n = arity(s.masked, m.range.last + 1)
                if (!has(ms, name, n)) off += "${s.name}:${s.lineOf(m.range.first)}: Masters.$name${if (n < 0) "" else "/$n"} does not exist"
            }
        }
        I18n.check("f · unresolved accessor", off, "the app calls an accessor the generated table does not have: check the key in android/i18n/index.tsv")
    }

    private val SYSTEM_LANGUAGE = Regex("""\bLocale\s*\.\s*getDefault\b|\bLocaleList\b|\.\s*locales\b|\bconfiguration\s*\.\s*locale\b""")

    @Test fun `g the app never reads the system language`() {
        val off = scans.filter { it.rel.startsWith("app/") }.flatMap { s ->
            SYSTEM_LANGUAGE.findAll(s.masked).map { "${s.name}:${s.lineOf(it.range.first)}: ${it.value}" }.toList()
        }
        I18n.check("g · system language", off, "the language comes from the भाषा · Language row alone; English on first run whatever the phone's language")
    }

    private val TX_MENTION = Regex("""(?<![\w.])tx\b|\bTx\b|\bLocalTx\b|\bMasters\b|\bMsg\b""")

    @Test fun `h the export and upload builders never see Tx`() {
        val off = ArrayList<String>()
        for (s in scans) {
            val region: IntRange? = when (s.name) {
                "PlanExport.kt", "FaceWriter.kt" -> 0 until s.masked.length
                "AsIsUpload.kt" -> {
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
            if (region != null && s.rel.startsWith("app/")) {
                TX_MENTION.findAll(s.masked.substring(region.first, region.last + 1)).forEach {
                    off += "${s.name}:${s.lineOf(region.first + it.range.first)}: ${it.value}"
                }
            }
        }
        listOf("PlanExport.kt", "FaceWriter.kt", "AsIsUpload.kt").filter { n -> scans.none { it.name == n } }.forEach { off += "$it: not found (update this check)" }
        I18n.check("h · Tx in an export or upload builder", off, "the PDF, the images and the upload body stay English (decision 4 of 21 Oct): they take no Tx")
    }

    @Test fun `the language row stays hidden until every screen is translated`() {
        val notPorted = scans.count { it.rel !in ported }
        val off = if (LangRow.VISIBLE && (unkeyed.isNotEmpty() || notPorted > 0))
            listOf("LangRow.VISIBLE is true with ${unkeyed.size} unkeyed rows and $notPorted files not in ported.txt") else emptyList()
        I18n.check("the language row", off, "the भाषा · Language row stays hidden until every screen is translated (Amit, 27 Oct 2026, decision 2)")
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
