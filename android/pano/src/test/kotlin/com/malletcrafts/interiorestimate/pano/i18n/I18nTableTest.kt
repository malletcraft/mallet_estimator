package com.malletcrafts.interiorestimate.pano.i18n

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * THE TABLE AND ITS GENERATED CODE (design-marathi §3.3, I18nTableTest 1–8; slice 1a). Every check
 * asserts a PROPERTY of the two tables or compares the committed files with what GenTx writes; none
 * compares the table with a copy of itself (I18nTestHygieneTest).
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
