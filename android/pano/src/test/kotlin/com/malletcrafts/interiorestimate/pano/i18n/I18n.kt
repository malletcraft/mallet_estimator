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

    /** android/i18n/mr-pending.tsv: the keys whose Marathi is still their English (port 63, English first). */
    const val MR_PENDING_TSV = "mr-pending.tsv"

    /**
     * THE MARATHI-PENDING LIST, as (key, English) rows in the file's order (port 63 slice M1; Amit, 16 Nov 2026:
     * "Let amit verify it end to end in English then only start Marathi transformation"). A key added for the survey
     * to quote journey is built and verified in English first: until Amit has verified the journey its value in
     * strings-mr.json IS its English, and it is listed here. TxRenderTest's two Latin scans skip these keys, AND ONLY
     * THESE; MrPendingTest holds the list honest, so it cannot become a way round the scans.
     */
    val pendingRows: List<Pair<String, String>> by lazy { LiteralScan.readTsv(i18n.resolve(MR_PENDING_TSV), "key") }

    /** The keys of [pendingRows]. */
    val pending: Set<String> by lazy { pendingRows.map { it.first }.toSet() }

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
