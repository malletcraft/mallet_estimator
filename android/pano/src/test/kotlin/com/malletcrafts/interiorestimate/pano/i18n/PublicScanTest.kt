package com.malletcrafts.interiorestimate.pano.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * THE PUBLIC-REPO RULES OF CHECKS 7 AND 8, on synthetic tables: every figure here is made up. They pin
 * that keys are scanned and reported by position only, and that a client-name list asked for but not
 * usable fails instead of quietly running without names.
 */
class PublicScanTest {

    private val pats by lazy { PublicScan.denyPatterns(I18n.i18n.resolve("deny-patterns.txt")).map { (n, rx) -> "'$n'" to rx } }

    @Test fun `a number left in a KEY is caught, and the key is not printed`() {
        val t = """{"x.ref_9000000000": {"en": "plain", "where": "a.js:1"}, "x.ok": {"en": "call 90000 00000"}}"""
        val off = PublicScan.offenders(PublicScan.leaves("en", t), pats)
        assertTrue(off.any { it.startsWith("en:entry #0 (a key; the key is withheld): matches 'phone'") }, off.joinToString("\n"))
        assertTrue(off.any { it.startsWith("en:entry #0 (a key; the key is withheld): matches 'digits10'") }, off.joinToString("\n"))
        assertTrue(off.none { "9000000000" in it }, off.joinToString("\n"))
        // a value hit in a clean entry names its path
        assertTrue("en:x.ok.en: matches 'phone'" in off, off.joinToString("\n"))
    }

    @Test fun `a nested key is scanned too, and withholds its entry`() {
        val t = """{"x.a": {"en": "{zorbleth}", "placeholders": {"zorbleth": "n"}}}"""
        val off = PublicScan.offenders(PublicScan.leaves("en", t), listOf("client-name entry #1" to PublicScan.nameRegex("Zorbleth")))
        assertTrue(off.isNotEmpty() && off.all { it.startsWith("en:entry #0 ") }, off.joinToString("\n"))
        assertTrue(off.none { "zorbleth" in it.lowercase() }, off.joinToString("\n"))
    }

    @Test fun `no client-name list is green on the patterns, and says so as a warning`() {
        for (env in listOf(null, "", "  ")) {
            val d = PublicScan.denyList(null, env)
            assertNull(d.names)
            assertEquals(emptyList(), d.problems)
            assertTrue(d.annotation!!.startsWith("::warning title=i18n · 8 · names::") && "NOT checked" in d.annotation!!, d.annotation)
        }
    }

    @Test fun `a list asked for that cannot be read fails, and never falls back to the secret`() {
        val missing = I18n.out.resolve("deny-test/no-such-file.txt").path
        for (d in listOf(PublicScan.denyList(missing, null), PublicScan.denyList(missing, "Zorbleth"), PublicScan.denyList(I18n.out.path, null))) {
            assertNull(d.names)
            assertEquals(1, d.problems.size, d.problems.joinToString())
            assertTrue("not a readable file" in d.problems[0], d.problems[0])
        }
    }

    @Test fun `a list with no usable name fails`() {
        val f = I18n.out.resolve("deny-test/empty.txt").also { it.parentFile.mkdirs(); it.writeText("# only a comment\nab\n") }
        val d = PublicScan.denyList(f.path, null)
        assertEquals(1, d.problems.size, d.problems.joinToString())
        assertEquals(1, PublicScan.denyList(null, "ab, x").problems.size)
    }

    @Test fun `a usable list is read from the file or the secret, and its count is noticed`() {
        val f = I18n.out.resolve("deny-test/names.txt").also { it.parentFile.mkdirs(); it.writeText("Zorbleth\n# a comment\nQorvaleth Demo\n") }
        val d = PublicScan.denyList(f.path, null)
        assertEquals(listOf("Zorbleth", "Qorvaleth Demo"), d.names)
        assertEquals(emptyList(), d.problems)
        assertTrue(d.annotation!!.startsWith("::notice title=i18n · 8 · names::2 client names checked"), d.annotation)
        assertEquals(listOf("Zorbleth", "Qorvaleth"), PublicScan.denyList(null, "Zorbleth, Qorvaleth").names)
    }
}
