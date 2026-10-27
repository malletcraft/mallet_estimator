package com.malletcrafts.interiorestimate.pano.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * COMPARING THE TABLE WITH ITSELF IS THE ONE WAY THESE TESTS COULD BECOME MEANINGLESS (design-marathi
 * §3.3). An `assertEquals(Tx.…)` or an expected value read from strings-*.json -- directly, or through
 * the model GenTx builds from them -- passes whatever the table says. Pins are literals; whole-table
 * checks assert properties.
 */
class I18nTestHygieneTest {

    private val SELF_COMPARE = Regex("""\bassertEquals\s*\(\s*(?:Tx|tx|Masters)\s*\.""")

    /** Every way a test reaches the tables: the files, the parser, and the model built from them (`I18n.model`). */
    private val READS_TABLES = listOf("strings-en.json", "strings-mr.json", "GenTx.TABLES", "parseTable(",
        "I18n.model", "modelOf(", "byKey", "GenTx.Model")

    /**
     * The files that may read the tables, each for a reason: I18n.kt builds the model; I18nTableTest asserts
     * properties of it and compares no value; TxRenderTest walks it for accessor names, kinds and parameter
     * types to render the whole table, and never reads an entry's text.
     */
    private val MAY_READ = setOf("I18n.kt", "I18nTableTest.kt", "TxRenderTest.kt")

    /** An entry's text read in TxRenderTest: `byKey….en`, or `e.mr` / `it.en` / `entry.mr` on a model entry. */
    private val VALUE_READ = Regex("""(?:\bbyKey\b[^\n]*|\b(?:e|it|entry|f)\b)\s*\.\s*(?:en|mr)\b""")

    /** What one test source (its path [rel] under android/) does wrong. */
    private fun offenders(rel: String, text: String): List<String> {
        val name = rel.substringAfterLast('/')
        val s = LiteralScan.scanText(rel, text)
        val off = ArrayList<String>()
        SELF_COMPARE.findAll(s.masked).forEach { off += "$rel:${s.lineOf(it.range.first)}: the expected value comes from the table" }
        if (name !in MAY_READ) READS_TABLES.filter { it in text }.forEach { off += "$rel: reads the tables ($it); only ${MAY_READ.sorted().joinToString()} may, and only for properties" }
        if (name == "I18nTableTest.kt" && Regex("""\bassertEquals\s*\(""").containsMatchIn(s.masked)) off += "$rel: I18nTableTest compares values; its checks assert properties"
        if (name == "TxRenderTest.kt") VALUE_READ.findAll(s.masked).forEach { off += "$rel:${s.lineOf(it.range.first)}: reads an entry's text from the model; a pin is a literal" }
        return off
    }

    @Test fun `no test compares the table with itself`() {
        val root = I18n.android.resolve("pano/src/test/kotlin")
        val off = ArrayList<String>()
        for (f in root.walkTopDown().filter { it.isFile && it.name.endsWith(".kt") }) {
            if (f.name == "I18nTestHygieneTest.kt") continue
            off += offenders(f.relativeTo(I18n.android).invariantSeparatorsPath, f.readText())
        }
        I18n.check("test hygiene", off, "a pin is a literal: write the expected English (or Marathi) out by hand")
    }

    @Test fun `the hygiene check sees an expected value taken from the model`() {
        val probe = "val want = I18n.model.byKey.getValue(\"app.display\").mr\nassertEquals(want, Tx.MR.app_display)\n"
        val rel = "pano/src/test/kotlin/com/malletcrafts/interiorestimate/pano/i18n/"
        assertTrue(offenders(rel + "SelfTest.kt", probe).isNotEmpty(), "a test outside MAY_READ that reads the model")
        assertTrue(offenders(rel + "TxRenderTest.kt", probe).any { "reads an entry's text" in it }, "TxRenderTest reading an entry's Marathi")
        assertTrue(offenders(rel + "SelfTest.kt", "assertEquals(Tx.MR.app_display, x)").isNotEmpty(), "Tx as the expected value")
        // and the property-only reads TxRenderTest does make stay clean -- `Tx.MR.mr` is the layout flag, not a text
        assertEquals(emptyList(), offenders(rel + "TxRenderTest.kt", "for (e in I18n.model.entries) if (e.kind == K && e.where.isNotEmpty()) f(e.accessor)\nassertTrue(Tx.MR.mr)"))
    }
}
