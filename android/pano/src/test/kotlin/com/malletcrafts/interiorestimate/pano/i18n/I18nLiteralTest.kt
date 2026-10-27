package com.malletcrafts.interiorestimate.pano.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * EVERY TEXT A PERSON SEES IS KEYED, LISTED OR MARKED (design-marathi §3.3, I18nLiteralTest a–h).
 * Scans every module in [LiteralScan.ROOTS] -- `:app`, `:camera` and `:pano` main, the generated table
 * excepted. This is also the local stand-in for the compiler of `:app` and `:camera` (neither builds
 * here) on the commonest break, an unresolved name (f): two of those broke `main` on 21 Sep.
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
        val found = formatsWithoutLocale(scans)
        val off = found.filter { it !in APP_FORMATS_PINNED } +
            (APP_FORMATS_PINNED - found).map { "$it: pinned but gone — remove it from APP_FORMATS_PINNED" }
        I18n.check("e · format without a Locale", off,
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

    private val SYSTEM_LANGUAGE = Regex("""\bLocale\s*\.\s*getDefault\b|\bLocaleList\b|\.\s*locales\b|\bconfiguration\s*\.\s*locale\b""")

    private fun systemLanguageReads(scans: List<LiteralScan.FileScan>): List<String> = scans.flatMap { s ->
        SYSTEM_LANGUAGE.findAll(s.masked).map { "${s.name}:${s.lineOf(it.range.first)}: ${it.value}" }.toList()
    }

    @Test fun `g no source reads the system language`() {
        // every module: :pano's redraw keys and exports would change bytes with the system language too
        I18n.check("g · system language", systemLanguageReads(scans), "the language comes from the भाषा · Language row alone; English on first run whatever the phone's language")
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
