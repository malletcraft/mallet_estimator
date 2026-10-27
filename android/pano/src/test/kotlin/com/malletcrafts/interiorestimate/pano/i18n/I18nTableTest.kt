package com.malletcrafts.interiorestimate.pano.i18n

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * THE TABLE AND ITS GENERATED CODE (design-marathi §3.3, I18nTableTest 1–8; slice 1a). Every check
 * asserts a PROPERTY of the two tables or compares the committed files with what GenTx writes; none
 * compares the table with a copy of itself (I18nTestHygieneTest). Check 9 (the slice 1c review) holds
 * the sources to one property of the table: a fragment a whole template supersedes is never glued.
 */
class I18nTableTest {

    private val m = I18n.model
    private val enText by lazy { I18n.i18n.resolve(GenTx.TABLES[0]).readText() }
    private val mrText by lazy { I18n.i18n.resolve(GenTx.TABLES[1]).readText() }
    private val en by lazy { GenTx.parseTable(enText, "en") }
    private val mr by lazy { GenTx.parseTable(mrText, "mr") }

    /** The 14 templates whose code picks an ENGLISH word for a placeholder (design §2.3). This list may only SHRINK, to empty. */
    private val PINNED_REFUSED = listOf(
        "bundle.aria.x_five_parts_x", "export.saved_x_x", "export.x_x_photo_not", "phase.aria.phase_x_4_x",
        "sp.aria.x_x_s_four", "sp.msg.x_x_item_x", "sp.office_view_x", "sp.placing_x_tap_x", "sp.x_all_x",
        "sp.x_corner", "sp.x_x_corner", "t53.no_x_marks_x", "wish.x_x_not_any", "wish.x_x_not_any_svc",
    )

    @Test fun `0 GenTx accepts the tables`() =
        I18n.check("GenTx", m.errors, "GenTx refuses to generate from android/i18n; fix the tables (or masters.tsv) it names")

    @Test fun `1 both tables have the same keys in the same order`() {
        val a = en.keys.toList(); val b = mr.keys.toList()
        val off = ArrayList<String>()
        (a - b.toSet()).forEach { off += "$it: only in strings-en.json" }
        (b - a.toSet()).forEach { off += "$it: only in strings-mr.json" }
        if (off.isEmpty() && a != b) off += "same keys, different order: first at ${a.indices.first { a[it] != b[it] }} (${a.first { k -> a.indexOf(k) != b.indexOf(k) }})"
        I18n.check("1 · keys", off, "the two tables must be mirrors, key for key and in order")
    }

    @Test fun `2 every generated key has Marathi`() {
        val markup = Regex("""</?[A-Za-z][^>]*>|&[a-z]+;|&#\d+;""")
        val off = ArrayList<String>()
        for (e in m.entries) {
            if (e.kind == GenTx.Kind.SKIPPED || e.kind == GenTx.Kind.REFUSED) continue
            val v = e.mr
            if (v.isEmpty() && e.key != GenTx.EMPTY_OK) off += "${e.key}: empty"
            if ('⟦' in v) off += "${e.key}: ⟦ in the Marathi"
            if (v.any { it in '०'..'९' }) off += "${e.key}: a Devanagari digit (digits stay Latin)"
            if (markup.containsMatchIn(v)) off += "${e.key}: markup in the Marathi"
        }
        I18n.check("2 · Marathi", off, "every key the phone can call needs Marathi: non-empty (sp.the excepted), no ⟦, Latin digits, no markup")
    }

    @Test fun `3 Marathi placeholders are a subset of the English`() {
        val off = ArrayList<String>()
        for (e in m.entries) {
            if (e.kind == GenTx.Kind.SKIPPED) continue
            @Suppress("UNCHECKED_CAST")
            val exprs = (en.getValue(e.key)["placeholders"] as? Map<String, Any?>) ?: emptyMap()
            val pe = GenTx.placeholders(e.en); val pm = GenTx.placeholders(e.mr)
            (pm - pe.toSet()).forEach { off += "${e.key}: {$it} is in the Marathi only" }
            for (d in pe - pm.toSet()) {
                val shape = exprs[d]?.let { GenTx.shapeOf(it.toString()) }
                if (shape !is GenTx.Shape.Suffix) off += "${e.key}: the Marathi drops {$d}, which is not a plural suffix"
            }
        }
        I18n.check("3 · placeholders", off, "a Marathi value may reorder the English placeholders and drop only a plural suffix")
    }

    @Test fun `4 GenTx refuses exactly the pinned English-alternative templates`() {
        val now = m.refused
        val off = (now - PINNED_REFUSED.toSet()).map { "$it: newly refused — ${m.byKey.getValue(it).why}" } +
            (PINNED_REFUSED - now.toSet()).map { "$it: no longer refused — remove it from PINNED_REFUSED (the list only shrinks)" }
        I18n.check("4 · refusals", off, "the refusal list is pinned and may only shrink as the prototype splits each template into two whole keys")
    }

    @Test fun `5 no two keys make one accessor, and none is a Tx member`() {
        val acc = m.accessors
        val off = ArrayList<String>()
        acc.groupBy { it }.filter { it.value.size > 1 }.keys.forEach { off += "$it: made by two keys" }
        acc.filter { it in GenTx.MEMBERS || '_' !in it }.forEach { off += "$it: collides with a Tx member" }
        val methods = Tx::class.java.methods.map { it.name }.toSet()
        acc.filter { a -> a !in methods && "get" + a.replaceFirstChar { it.uppercase() } !in methods }.forEach { off += "$it: not in the compiled Tx" }
        I18n.check("5 · accessors", off, "one accessor per key, every one compiled")
        assertTrue(acc.size > 1000, "the table has ${acc.size} accessors; it should hold the prototype's ~1,480")
    }

    @Test fun `6 the committed generated files are what GenTx writes`() {
        val off = ArrayList<String>()
        for ((rel, text) in GenTx.files(m)) {
            val f = I18n.android.resolve(rel)
            if (!f.isFile) off += "$rel: missing"
            else if (f.readText() != text) off += "$rel: stale"
        }
        I18n.check("6 · generated code", off, "run `gradle :pano:i18nGen` and commit what it writes (never edit a GENERATED file by hand)")
    }

    // ---------------------------------------------------------------- a whole sentence, never a glued fragment

    /** A prototype site, `v53build/app.js:460` of `v53build/app.js:460 · z`. */
    private fun site(where: String) = where.substringBefore(" · ").trim()

    /**
     * Fragment accessor -> the whole template that supersedes it: a fragment key whose English (trimmed,
     * as whole words) sits inside a template's English at the SAME prototype site. The prototype's display
     * layer tries a whole template before it glues a suffix, so at that site the fragment glued to a value
     * is the form the prototype rejected -- v57's `app.x_shot` exists because `app.shot` made "3 खोल्या फोटो"
     * where the chip reads "फोटो झाले: 3 खोल्या" (design §9: a fragment may stand alone as a label, never
     * be joined into a sentence). A property of the table: no text is compared.
     */
    private val superseded: Map<String, String> by lazy {
        val templates = m.entries.filter { it.kind == GenTx.Kind.TEMPLATE && it.accessor != null && it.where.isNotEmpty() }
            .groupBy { site(it.where) }
        val out = LinkedHashMap<String, String>()
        for (e in m.entries) {
            val acc = e.accessor ?: continue
            if ("fragment" !in e.flags || e.where.isEmpty()) continue
            if (e.kind != GenTx.Kind.STATIC && e.kind != GenTx.Kind.TEMPLATE) continue
            val words = e.en.trim(); if (words.isEmpty()) continue
            val rx = Regex("(?<![A-Za-z])" + Regex.escape(words) + "(?![A-Za-z])")
            templates[site(e.where)]?.firstOrNull { it.key != e.key && rx.containsMatchIn(it.en) }?.let { out[acc] = it.accessor!! }
        }
        out
    }

    private val TX_USE = Regex("""(?<![\w.])(?:tx|Tx\s*\.\s*(?:EN|MR))\s*\.\s*([A-Za-z_]\w*)""")

    /** Each use, in [scans], of a fragment [sup] supersedes that is JOINED: `+` or `+=` either side, or inside a string. */
    private fun gluedFragments(scans: List<LiteralScan.FileScan>, sup: Map<String, String>): List<String> {
        val off = ArrayList<String>()
        for (s in scans) {
            val t = s.masked
            for (u in TX_USE.findAll(t)) {
                val name = u.groupValues[1]; val whole = sup[name] ?: continue
                var a = u.range.first - 1; while (a >= 0 && t[a].isWhitespace()) a--
                var b = u.range.last + 1; while (b < t.length && t[b].isWhitespace()) b++
                if (b < t.length && t[b] == '(') {                       // a template fragment: skip its arguments
                    var d = 0
                    while (b < t.length) { if (t[b] == '(') d++ else if (t[b] == ')') { d--; if (d == 0) break }; b++ }
                    b++; while (b < t.length && t[b].isWhitespace()) b++
                }
                val before = a >= 0 && (t[a] == '+' || (t[a] == '=' && a >= 1 && t[a - 1] == '+'))
                val after = b < t.length && t[b] == '+'
                val inString = s.lits.any { u.range.first in it.start until it.end }
                if (before || after || inString) off += "${s.name}:${s.lineOf(u.range.first)}: tx.$name is glued into a sentence; the table has the whole one, tx.$whole"
            }
        }
        return off
    }

    @Test fun `9 no fragment a whole template supersedes is glued into a sentence`() {
        val scans = LiteralScan.sources(I18n.android).map { LiteralScan.scan(I18n.android, it) }
        I18n.check("9 · glued fragment", gluedFragments(scans, superseded),
            "call the whole template the prototype draws at that site (tx.app_x_shot(n), not tx.pl_room(n) + tx.app_shot): glued, the Marathi keeps the English word order")
    }

    @Test fun `9 the glued-fragment check sees each way of joining, and lets a fragment stand alone`() {
        // the table's own v57 case is found, and a fragment with no whole template at its site is not
        assertTrue(superseded["app_shot"] == "app_x_shot", "app.shot is superseded by app.x_shot at v53build/app.js:460")
        assertTrue("app_wi_fi_only" !in superseded, "app.wi_fi_only has no whole template at its site: it may still be glued")
        val src = "fun f() {\n" +
            "    Text(tx.pl_room(n) + tx.app_shot)\n" +
            "    Text(tx.app_shot + \"!\")\n" +
            "    Text(\"${'$'}{tx.pl_room(n)}${'$'}{tx.app_shot}\")\n" +
            "    s += Tx.EN.app_shot\n" +
            "    Text(tx.app_shot)\n" +
            "    Text(tx.app_x_shot(n))\n" +
            "    Text((if (w) tx.app_wi_fi_only else \"\") + tx.app_x_waiting(q = \"3\"))\n" +
            "}\n"
        val off = gluedFragments(listOf(LiteralScan.scanText("app/src/main/kotlin/X.kt", src)), mapOf("app_shot" to "app_x_shot"))
        val lines = off.map { it.substringAfter("X.kt:").substringBefore(':').toInt() }
        assertTrue(lines == listOf(2, 3, 4, 5), off.joinToString("\n"))
    }

    // ---------------------------------------------------------------- what may never enter a PUBLIC repo

    /** Every string in both table files: values, notes, locations and KEYS alike (keys outlive their text). */
    private val allLeaves by lazy { PublicScan.leaves("en", enText) + PublicScan.leaves("mr", mrText) }

    /**
     * ₹, Rs or INR (any case), an optional dot or space, and a digit; or a figure ending `/-`: a cost
     * figure (design §3.3, check 7). A real failure, not a "should". deny-patterns.txt's `currency` agrees.
     */
    private val CURRENCY = Regex("""(?i)(?:₹|(?<![a-z])(?:rs|inr))\.?\s?\d|\d[\d,]*\s?/-""")

    @Test fun `7 no cost figure in either table`() {
        val off = PublicScan.offenders(allLeaves, listOf("a currency figure" to CURRENCY))
        I18n.check("7 · cost figure", off, "these tables enter the PUBLIC mallet_estimator repo; no rate, cost or price may be in them")
    }

    /** The committed STRUCTURAL deny list, and the forms each required pattern must still catch (all made up). */
    private val REQUIRED = mapOf(
        "email" to listOf("write to someone@example.com"),
        "phone" to listOf("call 90000 00000", "call 090000 00000", "+91 90 0000 0000", "90000 000 00", "9000 000 000"),
        "landline" to listOf("office 020-0000 0000", "office 020 00000000"),
        "digits10" to listOf("ref 1234567890"),
        "currency" to listOf("Rs. 0 as a sample", "RS. 0 as a sample", "rs 0 as a sample", "₹ 0", "0/- as a sample"),
    )

    @Test fun `8 no client name, phone number, email or figure in either table`() {
        val pats = PublicScan.denyPatterns(I18n.i18n.resolve("deny-patterns.txt"))
        val off = ArrayList<String>()
        for ((name, samples) in REQUIRED) {
            val rx = pats[name]
            if (rx == null) off += "deny-patterns.txt: the required pattern '$name' is missing"
            else samples.filter { !rx.containsMatchIn(it) }.forEach { off += "deny-patterns.txt: '$name' no longer catches its own example '$it'" }
        }
        // The client-name list: absent is green on the structural patterns but SAYS so in the CI log; a
        // list asked for that cannot be used is a failure, never a quiet run without names.
        val deny = PublicScan.denyList(System.getProperty("mcft.i18n.deny"), System.getenv("I18N_DENY_NAMES"))
        off += deny.problems
        deny.annotation?.let { I18n.annotate(it); println("i18n check 8: $it") }
        // The matched text is NEVER printed: these logs are public. The key (or, when a key matched, only its
        // position) and the pattern's name say where to look.
        val rules = pats.map { (name, rx) -> "'$name'" to rx } +
            (deny.names ?: emptyList()).mapIndexed { i, n -> "client-name entry #${i + 1}" to PublicScan.nameRegex(n) }
        off += PublicScan.offenders(allLeaves, rules)
        I18n.check("8 · names and figures", off, "the tables enter the PUBLIC repo: remove the client name, phone, email or figure (the text itself is not printed)")
    }
}
