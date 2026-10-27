package com.malletcrafts.interiorestimate.pano.i18n

import java.io.File
import kotlin.test.fail

/** What the i18n tests share: where the tables are, the model GenTx builds, and how a check reports. */
internal object I18n {

    /** `android/`, from `systemProperty("mcft.android", rootDir)` in :pano's tasks.test. No default: a wrong guess would check the wrong tree. */
    val android: File by lazy {
        File(System.getProperty("mcft.android") ?: error("mcft.android is not set: run these through gradle :pano:test")).absoluteFile
    }
    val i18n: File get() = android.resolve("i18n")
    val model: GenTx.Model by lazy { GenTx.modelOf(i18n) }

    /** pano/build/i18n: the unused-accessor report and the CI annotations the i18nReport task prints. */
    val out: File by lazy {
        File(System.getProperty("mcft.i18n.out") ?: error("mcft.i18n.out is not set: run these through gradle :pano:test")).also { it.mkdirs() }
    }

    private fun esc(s: String) = s.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")

    /**
     * ONE assertion per check, holding EVERY offender, so a single red run lists the whole worklist;
     * and ONE GitHub annotation per check naming at most 25 (GitHub drops the eleventh annotation of a
     * level in a step, and the full list is in the assertion message anyway).
     */
    /** One line for the CI log that is not a failure (a `::warning` or `::notice`): i18nReport prints it at the start of a line. */
    fun annotate(line: String) {
        synchronized(this) { out.resolve("annotations.txt").appendText(line.replace("\n", " ") + "\n") }
    }

    fun check(name: String, offenders: Collection<String>, hint: String) {
        if (offenders.isEmpty()) return
        synchronized(this) {
            out.resolve("annotations.txt").appendText(
                "::error title=i18n · ${esc(name)}::${esc("${offenders.size}: $hint — ${offenders.take(25).joinToString(" | ")}")}\n")
        }
        fail("$name: ${offenders.size} — $hint\n  " + offenders.joinToString("\n  "))
    }
}
