package com.malletcrafts.interiorestimate.pano.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * HOW THE TABLE RENDERS (design-marathi §3.3). Literal pins only where the behaviour is the point;
 * everything else is a whole-table property, so a prototype wording change never means retyping
 * Marathi into a test.
 */
class TxRenderTest {

    @Test fun `a counted noun is the count and the right form`() {
        assertEquals("1 room", Tx.EN.pl_room(1))
        assertEquals("3 rooms", Tx.EN.pl_room(3))
        assertEquals("2 खोल्या", Tx.MR.pl_room(2))
    }

    @Test fun `a plural suffix folds into the count it agrees with`() {
        assertEquals("1 item cleared", Tx.EN.sp_x_item_x_cleared(1))
        assertEquals("3 items cleared", Tx.EN.sp_x_item_x_cleared(3))
        assertEquals("3 आयटम काढले", Tx.MR.sp_x_item_x_cleared(3))
        // the reversed suffix: "1 mark stays", "3 marks stay"
        assertEquals("1 Scope mark stays locked", Tx.EN.life_x_scope_mark_x(1))
        assertEquals("3 Scope marks stay locked", Tx.EN.life_x_scope_mark_x(3))
    }

    @Test fun `a word-only plural sharing a count shows the word alone`() {
        assertEquals("3 list pages", Tx.EN.export_x_list_x(3))
        assertEquals("1 list page", Tx.EN.export_x_list_x(1))
    }

    @Test fun `a reordered template with two plurals fills each by name`() {
        // Catches a count passed where a plural word belongs, which would print "3 over 2".
        assertEquals("3 photos over 2 days", Tx.EN.app_x_over_x(3, 2))
        // The Marathi puts the days first. Its wording ("2 दिवस मध्ये", which wants दिवसांत) is on the
        // v61c card, so only the ORDER is pinned here -- a full literal would freeze the fault.
        val mr = Tx.MR.app_x_over_x(3, 2)
        assertTrue(mr.startsWith("2 ") && mr.endsWith(" 3 फोटो"), mr)
    }

    @Test fun `an optional tail is nothing, or its separator and the value`() {
        assertEquals("No marks match", Tx.EN.life_no_marks_match_x(null))
        assertEquals("No marks match — FD1", Tx.EN.life_no_marks_match_x("FD1"))
    }

    @Test fun `a whole optional tail is passed whole, from its own fragment key`() {
        // The tail holds text after its value (" skipped", " already moved on", " of 3"), so the table
        // carries it whole as a fragment key, separator included, and nothing is put before it.
        assertEquals("3 marks → Client approved · 2 skipped", Tx.EN.export_x_x_client_approved(3, Tx.EN.export_x_skipped_2("2"), ""))
        assertEquals("3 मार्क → ग्राहक मंजूर · 2 तसेच राहिले", Tx.MR.export_x_x_client_approved(3, Tx.MR.export_x_skipped_2("2"), ""))
        assertEquals("3 marks → Vendor review · 2 skipped", Tx.EN.export_x_x_vendor_review(3, Tx.EN.export_x_skipped("2"), ""))
        assertEquals("3 मार्क → व्हेंडर तपासणी · 2 तसेच राहिले", Tx.MR.export_x_x_vendor_review(3, Tx.MR.export_x_skipped("2"), ""))
        assertEquals("List · N · T · 1 of 3", Tx.EN.export_list_x_x_x("N", "T", Tx.EN.export_x_x_2("1", "3")))
        assertEquals("यादी · N · T · 1 / 3", Tx.MR.export_list_x_x_x("N", "T", Tx.MR.export_x_x_2("1", "3")))
        assertEquals("Only when the client has said yes — signed copy or message. Record 3 marks of pack P as Client approved · 1 already moved on",
            Tx.EN.export_only_when_client_has_2(3, "P", Tx.EN.export_x_already_moved("1")))
        assertEquals("ग्राहकाने होकार दिल्यावरच — सही केलेली प्रत किंवा मेसेज. पॅक P चे 3 मार्क \"ग्राहक मंजूर\" म्हणून नोंदवा · 1 आधीच पुढे गेले",
            Tx.MR.export_only_when_client_has_2(3, "P", Tx.MR.export_x_already_moved("1")))
        assertEquals("3 marks → Client approved", Tx.EN.export_x_x_client_approved(3, null, ""))
    }

    @Test fun `a value holding braces is never filled again`() {
        assertEquals("Chapter 3: {n} {name}", Tx.EN.album_aria_chapter_x("3", "{n} {name}"))
    }

    @Test fun `the language is the stored choice alone, English unless exactly mr`() {
        assertSame(Tx.EN, Tx.of(null))
        assertSame(Tx.EN, Tx.of("en"))
        assertSame(Tx.EN, Tx.of("MR"))
        assertSame(Tx.EN, Tx.of("mr-IN"))
        assertSame(Tx.EN, Tx.of(""))
        assertSame(Tx.MR, Tx.of("mr"))
        assertTrue(Tx.MR.mr && !Tx.EN.mr)
    }

    // ---------------------------------------------------------------- the whole table

    /** The whole table rendered once per language and shared by the tests that read it. */
    private object Rendered {
        val en by lazy { renders(Tx.EN) }
        val mr by lazy { renders(Tx.MR) }
    }

    private fun latinOk(): List<Pair<String, List<String>>> = I18n.i18n.resolve("latin-ok.txt").readLines()
        .map { it.substringBefore('#').trimEnd() }.filter { it.isNotBlank() }
        .map { line -> val c = line.split('\t').map { it.trim() }.filter { it.isNotEmpty() }; c[0] to (c.getOrNull(1)?.split(',')?.map { it.trim() } ?: emptyList()) }

    private fun allowed(ok: List<Pair<String, List<String>>>, run: String, key: String) = ok.any { (tok, where) ->
        tok == run && (where.isEmpty() || where.any { w -> if (w.endsWith("*")) key.startsWith(w.dropLast(1)) else key == w })
    }

    private val run = Regex("[A-Za-z][A-Za-z0-9-]*")

    // One check per test, so a red first check never hides a second (design §3.3: a single red run lists
    // the whole worklist).

    @Test fun `no Latin word leaks into a Marathi rendering`() {
        val ok = latinOk()
        val diag = I18n.model.entries.filter { "diagnostic-log" in it.flags }.map { it.key }.toSet()
        val off = ArrayList<String>()
        for ((key, name, text) in Rendered.mr) {
            if (key in diag) continue     // the laser test's log: English by the 21 Oct decision
            run.findAll(text).map { it.value }.filter { !allowed(ok, it, key) }.distinct()
                .forEach { off += "$key ($name): '$it'" }
        }
        I18n.check("Latin in Marathi", off.distinct(), "a Latin word in a Marathi text: fix the table, or (for a unit, product, code or dated decision) add it to android/i18n/latin-ok.txt")
    }

    @Test fun `latin-ok txt lists only runs some Marathi rendering still uses`() {
        // the list itself is pruned: an entry nothing needs is a hole somebody will one day walk through
        val ok = latinOk()
        val used = Rendered.mr.flatMap { (key, _, text) -> run.findAll(text).map { it.value to key }.toList() }
        val stale = ok.filter { (tok, where) -> used.none { (r, key) -> r == tok && (where.isEmpty() || where.any { w -> if (w.endsWith("*")) key.startsWith(w.dropLast(1)) else key == w }) } }
        I18n.check("latin-ok.txt", stale.map { it.first + (if (it.second.isEmpty()) "" else " (${it.second.joinToString(",")})") },
            "android/i18n/latin-ok.txt lists a Latin run no Marathi rendering has any more: remove the line")
    }

    @Test fun `every English rendering fills every placeholder`() {
        val off = Rendered.en.filter { '{' in it.third || '}' in it.third }.map { "${it.first}: '${it.third}'" }
        I18n.check("unfilled placeholder", off, "a {placeholder} survived rendering; GenTx's fill or the table is wrong")
    }

    @Test fun `every Marathi rendering fills every placeholder`() {
        val off = Rendered.mr.filter { '{' in it.third || '}' in it.third }.map { "${it.first}: '${it.third}'" }
        I18n.check("unfilled placeholder (mr)", off, "a {placeholder} survived a Marathi rendering")
    }

    /**
     * Every optional tail, filled the way a caller fills it: with the fragment key the table keeps at the
     * SAME prototype location when there is one (the tail as the prototype prints it, separator included),
     * else ◇. A tail the generated code rebuilds as separator + value, but that is passed whole, prints
     * its separator twice -- which neither the Latin scan nor the placeholder scan can see.
     */
    @Test fun `no rendering doubles a separator`() {
        val frags = I18n.model.entries.filter { "fragment" in it.flags && (it.kind == GenTx.Kind.STATIC || it.kind == GenTx.Kind.TEMPLATE) && it.where.isNotEmpty() }.groupBy { it.where }
        val doubled = Regex("""([·—])\s+\1""")
        val off = ArrayList<String>()
        for (tx in listOf(Tx.EN, Tx.MR)) for (e in I18n.model.entries) {
            if (e.kind != GenTx.Kind.TEMPLATE || e.params.none { it.type == "String?" }) continue
            val frag = frags[e.where]?.firstOrNull { it.key != e.key }
            val tail = frag?.let { f -> if (f.kind == GenTx.Kind.STATIC) call(tx, f.accessor!!, emptyList(), 1) else call(tx, f.accessor!!, f.params.map { it.type }, 1) } ?: "◇"
            val text = call(tx, e.accessor!!, e.params.map { it.type }, 1, tail)
            if (doubled.containsMatchIn(text)) off += "${e.key} (${tx.lang}, filled from ${frag?.key ?: "◇"}): '$text'"
        }
        I18n.check("doubled separator", off, "an optional tail holding more than separator + value is the WHOLE tail from its own key: GenTx must not put a separator before it")
    }

    @Test fun `accessors no phone source calls yet are reported, not failed`() {
        val ids = HashSet<String>()
        val word = Regex("[A-Za-z_][A-Za-z0-9_]*")
        for (rel in LiteralScan.sources(I18n.android)) word.findAll(LiteralScan.scan(I18n.android, rel).masked).forEach { ids += it.value }
        val unused = I18n.model.accessors.filter { it !in ids }.sorted()
        I18n.out.resolve("unused.txt").writeText(unused.joinToString("\n", postfix = "\n"))
        println("i18n: ${unused.size} of ${I18n.model.accessors.size} accessors are not called by any phone source yet (pano/build/i18n/unused.txt)")
    }
}

private val TX_METHODS by lazy { Tx::class.java.methods.groupBy { it.name } }

/** Calls accessor [name] by reflection: counts 1 (variant 0) or 3, strings ◇, tails null (variant 0) or [tail]. */
private fun call(tx: Tx, name: String, types: List<String>, variant: Int, tail: String = "◇"): String {
    val args: Array<Any?> = types.map { t ->
        when (t) {
            "Int" -> if (variant == 0) 1 else 3
            "String" -> "◇"
            "String?" -> if (variant == 0) null else tail
            else -> error("Tx.$name: parameter type $t")
        }
    }.toTypedArray()
    val getter = "get" + name.replaceFirstChar { it.uppercase() }
    val m = (TX_METHODS[name] ?: TX_METHODS[getter])?.firstOrNull { it.parameterCount == types.size }
        ?: error("Tx.$name/${types.size} is not compiled")
    return m.invoke(tx, *args) as String
}

/** Every accessor of the model, rendered twice: counts 1 / strings ◇ / tails null, then counts 3 / ◇ / ◇. */
private fun renders(tx: Tx): List<Triple<String, String, String>> {
    val out = ArrayList<Triple<String, String, String>>()
    for (e in I18n.model.entries) for (variant in 0..1) when (e.kind) {
        GenTx.Kind.STATIC -> if (variant == 0) out += Triple(e.key, e.accessor!!, call(tx, e.accessor, emptyList(), 0))
        GenTx.Kind.TEMPLATE -> out += Triple(e.key, e.accessor!!, call(tx, e.accessor, e.params.map { it.type }, variant))
        GenTx.Kind.NOUN -> {
            out += Triple(e.key, e.accessor!!, call(tx, e.accessor, listOf("Int"), variant))
            out += Triple(e.key, GenTx.plName(e.key), call(tx, GenTx.plName(e.key), listOf("Int"), variant))
        }
        else -> {}
    }
    return out
}
