package com.malletcrafts.interiorestimate.pano.i18n

import kotlin.test.Test

/**
 * COMPARING THE TABLE WITH ITSELF IS THE ONE WAY THESE TESTS COULD BECOME MEANINGLESS (design-marathi
 * §3.3). An `assertEquals(Tx.…)` or an expected value read from strings-*.json passes whatever the
 * table says. Pins are literals; whole-table checks assert properties.
 */
class I18nTestHygieneTest {

    private val SELF_COMPARE = Regex("""\bassertEquals\s*\(\s*(?:Tx|tx|Masters)\s*\.""")
    private val READS_TABLES = listOf("strings-en.json", "strings-mr.json", "GenTx.TABLES", "parseTable(")

    @Test fun `no test compares the table with itself`() {
        val root = I18n.android.resolve("pano/src/test/kotlin")
        val off = ArrayList<String>()
        for (f in root.walkTopDown().filter { it.isFile && it.name.endsWith(".kt") }) {
            if (f.name == "I18nTestHygieneTest.kt") continue
            val rel = f.relativeTo(I18n.android).invariantSeparatorsPath
            val text = f.readText()
            val s = LiteralScan.scanText(rel, text)
            SELF_COMPARE.findAll(s.masked).forEach { off += "$rel:${s.lineOf(it.range.first)}: the expected value comes from the table" }
            if (f.name != "I18nTableTest.kt") READS_TABLES.filter { it in text }.forEach { off += "$rel: reads the tables ($it); only I18nTableTest may, and only for properties" }
            else if (Regex("""\bassertEquals\s*\(""").containsMatchIn(s.masked)) off += "$rel: I18nTableTest compares values; its checks assert properties"
        }
        I18n.check("test hygiene", off, "a pin is a literal: write the expected English (or Marathi) out by hand")
    }
}
