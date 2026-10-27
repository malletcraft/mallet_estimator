package com.malletcrafts.interiorestimate.pano.i18n

import kotlin.test.Test

/**
 * HOW pano/build.gradle.kts RUNS THESE CHECKS, read as text: neither fault fails a build on its own, so
 * nothing else would notice it coming back.
 */
class I18nBuildTest {

    private val script by lazy { I18n.android.resolve("pano/build.gradle.kts").readText() }

    /**
     * The tests read SOURCE TEXT (comments, `// text:value` markers, the generated files byte for byte,
     * the test files for hygiene). A comment-only edit changes no bytecode, so a tree the tests read but
     * Gradle does not see as an input leaves :pano:test UP-TO-DATE locally while CI, on a fresh checkout, fails.
     */
    @Test fun `every source tree the tests read is a test input`() {
        val want = LiteralScan.ROOTS.map { r -> if (r.startsWith("pano/")) "projectDir.resolve(\"${r.removePrefix("pano/")}\")" else "rootDir.resolve(\"$r\")" } +
            listOf("projectDir.resolve(\"src/test/kotlin\")", "projectDir.resolve(\"build.gradle.kts\")")
        val off = want.filter { w -> !Regex("""inputs\s*\.\s*(?:dir|file)\s*\(\s*""" + Regex.escape(w)).containsMatchIn(script) }
            .map { "tasks.test declares no input $it" }
        I18n.check("build · test inputs", off, "declare it in tasks.test (inputs.dir / inputs.file) beside the others, or a comment-only edit leaves :pano:test UP-TO-DATE")
    }

    /**
     * A CommandLineArgumentProvider that reads a script-level val or fun captures the build script object,
     * which the configuration cache cannot store: i18nImport and i18nCheckProto did their work and the
     * build still went red. Each provider reads locals of its own register block only.
     */
    @Test fun `no argument provider reads a script-level name`() {
        val top = Regex("""(?m)^(?:val|var|fun)\s+([A-Za-z_]\w*)""").findAll(script).map { it.groupValues[1] }.toSet()
        val off = ArrayList<String>()
        for (m in Regex("""CommandLineArgumentProvider\s*\{""").findAll(script)) {
            var d = 0; var i = m.range.last
            while (i < script.length) { if (script[i] == '{') d++ else if (script[i] == '}') { d--; if (d == 0) break }; i++ }
            val body = script.substring(m.range.last, minOf(i + 1, script.length))
            val line = script.substring(0, m.range.first).count { it == '\n' } + 1
            top.filter { Regex("""\b${Regex.escape(it)}\b""").containsMatchIn(body) }.forEach { off += "build.gradle.kts:$line: the provider reads the script-level '$it'" }
        }
        I18n.check("build · configuration cache", off, "read it into a local of the task's register block first (val x = ...), and use only that local inside the provider")
    }
}
