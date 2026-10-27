package com.malletcrafts.interiorestimate.pano.i18n

import java.io.File
import kotlin.system.exitProcess

/**
 * Every string literal in the phone's Kotlin, and whether a person sees it (design-marathi §3.3,
 * I18nLiteralTest). A port of the readers' `ktstrings.py` / `extract_kt.py` tokenizer: comments and
 * char literals skipped, `${}` nesting kept, literals nested inside a template emitted too.
 *
 * A literal is in a UI POSITION (design 0's narrow classification) when it is an argument --
 * directly, inside an if/when that is the argument, or through a template or `+` -- of `Text(`,
 * `DrawerLine(`, `DrawerGroup(`, `say(`, `snack(`, `Banner(` or `drawText(`, or the value of
 * `label =`, `title =`, `text =`, `placeholder =`, `contentDescription =`, `stateDescription =`, or
 * of an assignment to `lastResult`, `busy` or `note` -- a String, or (slice 1c) a `Msg` lambda
 * assigned to one, `lastResult = { tx -> … }`, whose value is the shown text. Everything else is
 * counted, not judged: the per-file ratchet of PROSE literals ([prose]).
 *
 *   ratchet <androidDir>    lower android/i18n/literal-counts.tsv to the counts in the sources
 */
object LiteralScan {

    val UI_CALLS = setOf("Text", "DrawerLine", "DrawerGroup", "say", "snack", "Banner", "drawText")
    val UI_NAMES = setOf("label", "title", "text", "placeholder", "contentDescription", "stateDescription", "lastResult", "busy", "note")

    /**
     * The source trees scanned, relative to `android/`: every module whose texts reach a screen. `:camera`
     * is compiled only into the camera build (the one Play ships), and its connect and capture messages
     * are shown on the capture sheet, so it is scanned like the others although nothing compiles it here.
     */
    val ROOTS = listOf("app/src/main/kotlin", "camera/src/main/kotlin", "pano/src/main/kotlin")

    const val COUNTS_TSV = "literal-counts.tsv"
    const val UNKEYED_TSV = "unkeyed.tsv"
    const val PORTED_TXT = "ported.txt"

    /** One string literal. [raw] is its body exactly as written between the quotes. */
    class Lit(
        val file: String, val start: Int, val end: Int, val line: Int,
        val raw: String, val triple: Boolean, val depth: Int,
        /** The body as text, escapes decoded, every `${…}` and `$name` removed. */
        val text: String,
    ) {
        var ui: Boolean = false
            internal set
        /** The body as one TSV cell: a real newline or tab written as `\n` / `\t`. */
        val cell: String get() = tsv(raw)
    }

    fun tsv(s: String) = s.replace("\t", "\\t").replace("\n", "\\n")

    class FileScan(val rel: String, val src: String, val masked: String, val lits: List<Lit>) {
        private val lineStarts: IntArray by lazy {
            val l = ArrayList<Int>(); l += 0
            src.forEachIndexed { i, c -> if (c == '\n') l += i + 1 }
            l.toIntArray()
        }
        fun lineOf(pos: Int): Int { var i = lineStarts.binarySearch(pos); if (i < 0) i = -i - 2; return i + 1 }
        fun lineText(line: Int): String {
            val s = lineStarts[line - 1]; val e = if (line < lineStarts.size) lineStarts[line] - 1 else src.length
            return src.substring(s, e)
        }
        /** Marked on its own line `// text:value <why>`: a product name, a unit, a code, or English by a dated decision. */
        fun marked(l: Lit): Boolean = lineText(l.line).contains("// text:value")
        val name: String get() = rel.substringAfterLast('/')
    }

    // ------------------------------------------------------------------ the lexer

    enum class T { IDENT, OP, NL, STR }
    class Tok(val t: T, val s: String, val pos: Int, val lit: Lit? = null, val tpl: List<List<Tok>> = emptyList())

    private val OPS = listOf("===", "!==", "?.", "?:", "->", "==", "!=", "<=", ">=", "&&", "||", "++", "--",
        "+=", "-=", "*=", "/=", "%=", "::", "..", "!!")

    private class Lexer(val src: String, val rel: String) {
        val lits = ArrayList<Lit>()
        val mask = src.toCharArray()
        private val n = src.length
        private val nl: IntArray = src.indices.filter { src[it] == '\n' }.toIntArray()
        fun lineOf(pos: Int): Int { var i = nl.binarySearch(pos); if (i < 0) i = -i - 1; return i + 1 }

        private fun blank(a: Int, b: Int) { for (k in a until b) if (mask[k] != '\n') mask[k] = ' ' }

        /** Tokens from [from]; with [inTemplate], stops after the `}` that closes the template. */
        fun tokens(from: Int, inTemplate: Boolean, depth: Int): Pair<List<Tok>, Int> {
            val out = ArrayList<Tok>()
            var i = from
            var braces = 0
            while (i < n) {
                val c = src[i]
                when {
                    c == '\n' -> { out += Tok(T.NL, "\n", i); i++ }
                    c == ' ' || c == '\t' || c == '\r' -> i++
                    src.startsWith("//", i) -> { val j = src.indexOf('\n', i).let { if (it < 0) n else it }; blank(i, j); i = j }
                    src.startsWith("/*", i) -> {
                        var d = 1; var j = i + 2
                        while (j < n && d > 0) {
                            if (src.startsWith("/*", j)) { d++; j += 2 } else if (src.startsWith("*/", j)) { d--; j += 2 } else j++
                        }
                        blank(i, j); i = j
                    }
                    c == '\'' -> {
                        var j = i + 1
                        while (j < n && src[j] != '\'' && src[j] != '\n') { if (src[j] == '\\') j++; j++ }
                        out += Tok(T.IDENT, src.substring(i, minOf(j + 1, n)), i); i = j + 1
                    }
                    c == '"' -> { val (tok, after) = string(i, depth); out += tok; i = after }
                    c.isLetter() || c == '_' -> {
                        var j = i + 1
                        while (j < n && (src[j].isLetterOrDigit() || src[j] == '_')) j++
                        out += Tok(T.IDENT, src.substring(i, j), i); i = j
                    }
                    c == '`' -> { val j = src.indexOf('`', i + 1).let { if (it < 0) n - 1 else it }; out += Tok(T.IDENT, src.substring(i + 1, j), i); i = j + 1 }
                    c.isDigit() -> {
                        var j = i + 1
                        while (j < n && (src[j].isLetterOrDigit() || src[j] == '_' || (src[j] == '.' && j + 1 < n && src[j + 1].isDigit()))) j++
                        out += Tok(T.IDENT, src.substring(i, j), i); i = j
                    }
                    else -> {
                        if (inTemplate) {
                            if (c == '{') braces++
                            if (c == '}') { if (braces == 0) return out to i + 1; braces-- }
                        }
                        val op = OPS.firstOrNull { src.startsWith(it, i) } ?: c.toString()
                        out += Tok(T.OP, op, i); i += op.length
                    }
                }
            }
            return out to i
        }

        /** A string literal at [i] (its first quote): the literal, its templates' tokens, and the index after it. */
        fun string(i: Int, depth: Int): Pair<Tok, Int> {
            val triple = src.startsWith("\"\"\"", i)
            val bodyStart = i + if (triple) 3 else 1
            var j = bodyStart
            val text = StringBuilder()
            val tpls = ArrayList<List<Tok>>()
            val code = ArrayList<IntRange>()
            var bodyEnd = n; var end = n
            while (j < n) {
                val c = src[j]
                if (triple && src.startsWith("\"\"\"", j)) {
                    var k = j; while (k < n && src[k] == '"') k++
                    repeat(k - j - 3) { text.append('"') }
                    bodyEnd = k - 3; end = k; break
                }
                if (!triple && c == '"') { bodyEnd = j; end = j + 1; break }
                if (!triple && c == '\n') { bodyEnd = j; end = j; break }          // unterminated: stop at the line
                if (!triple && c == '\\' && j + 1 < n) {
                    when (val e = src[j + 1]) {
                        'n' -> text.append('\n'); 't' -> text.append('\t'); 'r' -> text.append('\r'); 'b' -> text.append('\b')
                        'u' -> { if (j + 5 < n) src.substring(j + 2, j + 6).toIntOrNull(16)?.let { text.append(it.toChar()) }; j += 6; continue }
                        else -> text.append(e)
                    }
                    j += 2; continue
                }
                if (c == '$' && j + 1 < n && src[j + 1] == '{') {
                    val (toks, after) = tokens(j + 2, true, depth + 1)
                    tpls += toks; code += j until after; j = after; continue
                }
                if (c == '$' && j + 1 < n && (src[j + 1].isLetter() || src[j + 1] == '_')) {
                    var k = j + 1; while (k < n && (src[k].isLetterOrDigit() || src[k] == '_')) k++
                    code += j until k; j = k; continue
                }
                text.append(c); j++
            }
            // Blank the literal's own text in the mask; its templates stay code (their literals blank themselves).
            var k = bodyStart
            for (r in code.sortedBy { it.first }) { blank(k, r.first); k = r.last + 1 }
            blank(k, bodyEnd)
            val lit = Lit(rel, i, end, lineOf(i), src.substring(bodyStart, bodyEnd), triple, depth, text.toString())
            lits += lit
            return Tok(T.STR, "\"", i, lit, tpls) to end
        }
    }

    // ------------------------------------------------------------------ the walker

    private sealed class Frame {
        class Paren(val callee: String?) : Frame() { var arg: String? = null }
        object Bracket : Frame()
        class Block(val ctl: Boolean, val isWhen: Boolean) : Frame() { var target: String? = null; var arrow = false; var valName: String? = null }
        class Inherit(val ui: Boolean) : Frame()
    }

    private val CONT_PREV = setOf("=", "+", "-", "*", "/", "%", "&&", "||", "?:", ".", "?.", "->", ",", "(", "[", "==", "!=", "<=", ">=", "!", "+=", "-=", "::", "..", "===", "!==")
    private val CONT_PREV_WORDS = setOf("else", "in", "is", "as", "to", "and", "or")
    private val CONT_NEXT = setOf(".", "?.", "?:", "&&", "||")
    private val CTL = setOf("if", "when", "for", "while", "catch")
    /** A lambda whose value IS the expression's value (`server.ifBlank { "not set" }`): it inherits the position. */
    private val VALUE_LAMBDAS = setOf("ifBlank", "ifEmpty", "let", "run", "getOrElse")
    /**
     * The transient messages, which hold a `Msg` (design-marathi §2.4, slice 1c): a lambda assigned to
     * one (`lastResult = { tx -> … }`) is drawn later, and its value is the text a person sees, so it
     * inherits the assignment's UI position like [VALUE_LAMBDAS] do.
     */
    private val MSG_TARGETS = setOf("lastResult", "busy", "note")

    private fun classify(stack: List<Frame>): Boolean {
        for (f in stack.asReversed()) when (f) {
            is Frame.Paren -> return f.arg in UI_NAMES || f.callee in UI_CALLS
            is Frame.Bracket -> return false
            is Frame.Block -> {
                f.target?.let { return it in UI_NAMES }
                if (f.isWhen && !f.arrow) return false          // a when's condition, not its value
                if (!f.ctl) return false                          // a lambda or a body
            }
            is Frame.Inherit -> return f.ui
        }
        return false
    }

    private fun walk(toks: List<Tok>, base: Frame) {
        // The root block inherits: a file's top level from Inherit(false), a template's code from its literal.
        val stack = ArrayList<Frame>(); stack += base; stack += Frame.Block(ctl = true, isWhen = false)
        var lastClosed: String? = null
        fun sigAt(i: Int, step: Int): Int { var k = i + step; while (k in toks.indices && toks[k].t == T.NL) k += step; return k }
        fun sig(i: Int, step: Int): Tok? = toks.getOrNull(sigAt(i, step))
        fun calleeBefore(i: Int): String? {
            val pk = sigAt(i, -1)
            val p = toks.getOrNull(pk) ?: return null
            if (p.t == T.IDENT) return p.s
            if (p.t == T.OP && p.s == ">") {
                var d = 0; var k = pk
                while (k >= 0) {
                    val t = toks[k]
                    if (t.t == T.OP && t.s == ">") d++ else if (t.t == T.OP && t.s == "<") { d--; if (d == 0) break }
                    k--
                }
                return toks.getOrNull(k - 1)?.takeIf { it.t == T.IDENT }?.s
            }
            return null
        }
        for ((i, tok) in toks.withIndex()) {
            val top = stack.last()
            when (tok.t) {
                T.STR -> {
                    val lit = tok.lit!!
                    lit.ui = classify(stack)
                    for (tpl in tok.tpl) walk(tpl, Frame.Inherit(lit.ui))
                }
                T.NL -> if (top is Frame.Block) {
                    val p = sig(i, -1); val q = sig(i, 1)
                    val cont = (p != null && ((p.t == T.OP && p.s in CONT_PREV) || (p.t == T.IDENT && p.s in CONT_PREV_WORDS))) ||
                        (q != null && ((q.t == T.OP && q.s in CONT_NEXT) || (q.t == T.IDENT && q.s == "else")))
                    if (!cont) { top.target = null; top.arrow = false; top.valName = null }
                }
                T.IDENT -> if (top is Frame.Block) {
                    val p = sig(i, -1)
                    if (p != null && p.t == T.IDENT && (p.s == "val" || p.s == "var")) top.valName = tok.s
                }
                T.OP -> when (tok.s) {
                    "(" -> stack += Frame.Paren(calleeBefore(i))
                    ")" -> { val k = stack.indexOfLast { it is Frame.Paren }; if (k > 1) { lastClosed = (stack[k] as Frame.Paren).callee; while (stack.size > k) stack.removeAt(stack.size - 1) } }
                    "[" -> stack += Frame.Bracket
                    "]" -> { val k = stack.indexOfLast { it is Frame.Bracket }; if (k > 1) while (stack.size > k) stack.removeAt(stack.size - 1) }
                    "{" -> {
                        val p = sig(i, -1)
                        val (ctl, isWhen) = when {
                            p == null -> false to false
                            p.t == T.OP && p.s == ")" && lastClosed in CTL -> true to (lastClosed == "when")
                            p.t == T.IDENT && p.s in setOf("else", "try", "finally", "do") -> true to false
                            p.t == T.IDENT && p.s == "when" -> true to true
                            p.t == T.OP && p.s == "->" -> true to false
                            p.t == T.IDENT && p.s in VALUE_LAMBDAS -> true to false
                            p.t == T.OP && p.s == "=" && top is Frame.Block && top.target in MSG_TARGETS -> true to false
                            else -> false to false
                        }
                        stack += Frame.Block(ctl, isWhen)
                    }
                    "}" -> { val k = stack.indexOfLast { it is Frame.Block }; if (k > 1) while (stack.size > k) stack.removeAt(stack.size - 1) }
                    "," -> if (top is Frame.Paren) top.arg = null
                    ";" -> if (top is Frame.Block) { top.target = null; top.arrow = false; top.valName = null }
                    "->" -> if (top is Frame.Block) top.arrow = true
                    "=" -> {
                        val pk = sigAt(i, -1); val p = toks.getOrNull(pk)
                        if (top is Frame.Paren) {
                            val pp = toks.getOrNull(sigAt(pk, -1))
                            if (p != null && p.t == T.IDENT && pp != null && pp.t == T.OP && (pp.s == "(" || pp.s == ",")) top.arg = p.s
                        } else if (top is Frame.Block) {
                            top.target = top.valName ?: p?.takeIf { it.t == T.IDENT }?.s
                        }
                    }
                }
            }
        }
    }

    /** Lexes and classifies one source file ([rel] relative to [android]). */
    fun scan(android: File, rel: String): FileScan = scanText(rel, android.resolve(rel).readText())

    fun scanText(rel: String, src: String): FileScan {
        val lx = Lexer(src, rel)
        val (toks, _) = lx.tokens(0, false, 0)
        walk(toks, Frame.Inherit(false))
        return FileScan(rel, src, String(lx.mask), lx.lits.sortedBy { it.start })
    }

    /** A GENERATED file is the table itself, never scanned. */
    fun generated(f: File): Boolean = f.useLines { it.firstOrNull()?.startsWith("// GENERATED by GenTx") == true }

    /** Every phone source, relative to [android], minus the generated table. */
    fun sources(android: File): List<String> = ROOTS.flatMap { root ->
        val dir = android.resolve(root)
        require(dir.isDirectory) { "no $dir" }
        dir.walkTopDown().filter { it.isFile && it.name.endsWith(".kt") && !generated(it) }
            .map { it.relativeTo(android).invariantSeparatorsPath }.sorted().toList()
    }

    /** A Latin word: two Latin letters together, once the templates are gone. */
    private val WORD = Regex("[A-Za-z]{2,}")
    fun latinWord(l: Lit): Boolean = WORD.containsMatchIn(l.text)

    /** PROSE (§3.3 b): a Latin word beside a space, or a capitalised word. Ids and JSON keys (`wifi_only`) never count. */
    private val PROSE = Regex("""[A-Za-z]{2,}\s|\s[A-Za-z]{2,}|(?<![A-Za-z0-9_])[A-Z][a-z]+(?![A-Za-z0-9_])""")
    fun prose(l: Lit): Boolean = PROSE.containsMatchIn(l.text)

    /**
     * Whether a literal counts against its file's ratchet (§3.3 b): prose OUTSIDE a UI position that is
     * neither marked on its line `// text:value <why>` nor listed in unkeyed.tsv ([listed], file to cell).
     * The two remedies the check names must lower the count, or the only way to land a log line or a
     * wire value would be to raise literal-counts.tsv by hand, which nothing may do.
     */
    fun counted(s: FileScan, l: Lit, listed: Set<Pair<String, String>>): Boolean =
        !l.ui && prose(l) && !s.marked(l) && (s.rel to l.cell) !in listed

    /** android/i18n/unkeyed.tsv as (file, cell) pairs. */
    fun unkeyed(android: File): Set<Pair<String, String>> = readTsv(android.resolve("i18n/$UNKEYED_TSV"), "file").toSet()

    /** Per-file count of the literals [counted] counts. */
    fun counts(android: File): Map<String, Int> {
        val listed = unkeyed(android)
        return sources(android).associateWith { rel -> scan(android, rel).let { s -> s.lits.count { counted(s, it, listed) } } }
    }

    /** Reads a two-column TSV (comments `#`, a header row starting with [header]). */
    fun readTsv(f: File, header: String): List<Pair<String, String>> = f.readLines()
        .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("$header\t") }
        .map { val i = it.indexOf('\t'); require(i > 0) { "${f.name}: not 'a<TAB>b': $it" }; it.substring(0, i) to it.substring(i + 1) }

    const val COUNTS_HEAD = "# Per-file count of PROSE literals OUTSIDE UI positions (design-marathi §3.3 b): a Latin word beside a\n" +
        "# space, or a capitalised word, that is neither listed in unkeyed.tsv nor marked `// text:value <why>`.\n" +
        "# I18nLiteralTest fails when a file's count differs. A rise means a new literal: key it, list it in\n" +
        "# unkeyed.tsv (a text a person sees), or mark its line `// text:value <why>` (a log line, a wire value,\n" +
        "# a code); each of the three lowers the count. A fall: run gradle :pano:i18nRatchet, which only ever\n" +
        "# LOWERS these counts. Never raise one by hand.\n"

    fun writeCounts(f: File, counts: Map<String, Int>) {
        val sb = StringBuilder(COUNTS_HEAD).append("file\tcount\n")
        for ((k, v) in counts.toSortedMap()) sb.append(k).append('\t').append(v).append('\n')
        f.writeText(sb.toString())
    }

    private fun ratchet(android: File): Int {
        val f = android.resolve("i18n/$COUNTS_TSV")
        val old = readTsv(f, "file").associate { it.first to it.second.toInt() }
        val now = counts(android)
        val rose = now.filter { (k, v) -> v > (old[k] ?: 0) }
        val next = now.mapValues { (k, v) -> minOf(v, old[k] ?: 0) }
        writeCounts(f, next)
        val fell = next.filter { (k, v) -> v < (old[k] ?: 0) }
        println("i18nRatchet: ${fell.size} counts lowered${if (fell.isEmpty()) "" else ": $fell"}; ${(old.keys - now.keys).size} files gone")
        if (rose.isNotEmpty()) {
            System.err.println("i18nRatchet never raises a count. These files have MORE prose literals than listed -- key them, list them in unkeyed.tsv (a shown text) or mark their lines // text:value <why> (a log line, a wire value); any of the three lowers the count: $rose")
            return 1
        }
        return 0
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val code = when (args.firstOrNull()) {
            "ratchet" -> ratchet(File(args[1]))
            else -> { System.err.println("usage: ratchet <androidDir>"); 2 }
        }
        if (code != 0) exitProcess(code)
    }
}
