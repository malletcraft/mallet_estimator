package com.malletcrafts.interiorestimate.pano.i18n

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * THE MARATHI-PENDING LIST IS HONEST (port 63 slice M1, 16 Nov 2026; DESIGN-port63 §3). Amit, 16 Nov 2026: "Survey to
 * Quote journey is priority. Let amit verify it end to end in English then only start Marathi transformation." So a
 * key added for the journey carries its ENGLISH as its Marathi value for the time being, and is listed in
 * android/i18n/mr-pending.tsv (`key<TAB>English`); TxRenderTest's two Latin scans skip the listed keys.
 *
 * That skip is the one hole in the scans, and this test is what keeps it the size of the list: a listed key must be
 * in both tables with a Marathi value that is BYTE FOR BYTE its English, and both must be the list's own second
 * column. So a key whose Marathi has been written cannot stay listed (its Marathi no longer equals its English: the
 * commit that translates it takes it off the list, and from then on the scans read it), a key that was never added
 * cannot be listed, and no existing key with real Marathi can be put behind the skip. It asserts a property of the
 * pair against a hand-written file; no text is pinned from the tables (I18nTestHygieneTest lets it read them for this).
 */
class MrPendingTest {

    private val file by lazy { I18n.i18n.resolve(I18n.MR_PENDING_TSV) }
    private val en by lazy { GenTx.parseTable(I18n.i18n.resolve(GenTx.TABLES[0]).readText(), "en") }
    private val mr by lazy { GenTx.parseTable(I18n.i18n.resolve(GenTx.TABLES[1]).readText(), "mr") }

    @Test fun `the list says what it is waiting for`() {
        assertTrue(file.isFile, "android/i18n/${I18n.MR_PENDING_TSV} is missing")
        val head = file.readLines().takeWhile { it.startsWith("#") }.joinToString(" ")
        assertTrue(head.isNotBlank() && "VERIFICATION" in head.uppercase() && "ENGLISH" in head.uppercase(),
            "the list's # header says these keys wait for Amit's end-to-end verification of the journey in English")
        assertTrue(file.readLines().any { it == "key\tEnglish" }, "the list's header row is key<TAB>English")
    }

    @Test fun `no key is listed twice`() {
        val twice = I18n.pendingRows.groupBy { it.first }.filter { it.value.size > 1 }.keys
        I18n.check("mr-pending · twice", twice.toList(), "a key is listed once in android/i18n/mr-pending.tsv")
    }

    @Test fun `every listed key is in both tables, and its Marathi is still its English, byte for byte`() {
        val off = ArrayList<String>()
        for ((key, listed) in I18n.pendingRows) {
            val e = en[key]?.get("en") as? String; val m = mr[key]?.get("mr") as? String
            when {
                e == null -> off += "$key: not in strings-en.json"
                m == null -> off += "$key: not in strings-mr.json"
                e != listed -> off += "$key: the list's English is not the table's"
                m != e -> off += "$key: its Marathi is no longer its English -- it has been translated: take it off the list"
            }
        }
        I18n.check("mr-pending · values", off,
            "android/i18n/mr-pending.tsv lists only keys whose Marathi is still their English: key<TAB>English, exactly as strings-en.json has it")
    }

    @Test fun `a listed key is a phone text of its own -- never one the prototype's card translated`() {
        // the list can only ever name a key the phone added in English: an existing key has Marathi that is not its
        // English (the check above), and a prototype key has a prototype site, not a phone source
        val off = I18n.pendingRows.map { it.first }.filter { k ->
            "phone-text" !in ((en[k]?.get("flags") as? List<*>)?.map { it.toString() } ?: emptyList()) }
        I18n.check("mr-pending · phone texts", off, "a key waiting for its Marathi is a phone text (flag phone-text) added in English")
    }

    @Test fun `port 63 slice Q1 -- each of a four-corner mark's fourteen keys is in both tables, and listed for as long as its Marathi is its English`() {
        // THE LIST IS COMPLETE FOR THE SLICE: a key added in English and left OFF the list would be read by TxRenderTest's
        // Latin scans as Marathi with English in it. Held here by name, as a property that outlives the Marathi pass: a
        // key is either still waiting (listed, its Marathi its English) or translated (its Marathi no longer its English)
        val q1 = listOf("measure.side_bottom", "measure.side_right", "measure.side_top", "measure.side_left", "measure.side_back", "measure.side_front",
            "measure.diagonal", "measure.diagonal_plan", "measure.to_nearest_corner", "sp.shape.four_corners", "sp.shape.rectangle", "sp.shape.drag_corners",
            "sp.measure.sides_say_x", "sp.file.x_mark_x_shape_unreadable")
        val off = ArrayList<String>()
        for (k in q1) {
            val e = en[k]?.get("en") as? String; val m = mr[k]?.get("mr") as? String
            when {
                e == null || m == null -> off += "$k: not in both tables"
                m == e && k !in I18n.pending -> off += "$k: its Marathi is still its English and it is not listed"
                "phone-text" !in ((en[k]?.get("flags") as? List<*>)?.map { it.toString() } ?: emptyList()) -> off += "$k: not flagged phone-text"
            }
        }
        I18n.check("mr-pending · Q1", off, "a key added in English for a four-corner mark is listed in android/i18n/mr-pending.tsv until its Marathi is written")
        assertTrue(q1.size == 14 && q1.toSet().size == 14)
    }

    @Test fun `the Latin scans skip exactly the listed keys`() {
        // TxRenderTest reads the skip from the same list this test holds: it names I18n.pending in both scans and
        // skips by nothing else new -- read from its own source, so a second, wider skip cannot be added quietly
        val src = I18n.android.resolve("pano/src/test/kotlin/com/malletcrafts/interiorestimate/pano/i18n/TxRenderTest.kt").readText()
        fun body(name: String): String {
            val a = src.indexOf("fun `$name`"); assertTrue(a >= 0, "TxRenderTest has the scan `$name`")
            val b = src.indexOf("@Test", a).let { if (it < 0) src.length else it }
            return src.substring(a, b)
        }
        val leak = body("no Latin word leaks into a Marathi rendering")
        val stale = body("latin-ok txt lists only runs some Marathi rendering still uses")
        assertTrue("if (key in pending) continue" in leak && "val pending = I18n.pending" in leak, "the leak scan skips the pending keys")
        assertTrue("it.first !in pending" in stale && "val pending = I18n.pending" in stale, "the stale-entry scan skips the pending keys")
        // ... and the leak scan has exactly two skips: the laser test's log lines, and this list
        assertTrue(Regex("""\)\s*continue\b""").findAll(leak).count() == 2, "the leak scan skips two things: diagnostic-log keys and the pending list")
    }
}
