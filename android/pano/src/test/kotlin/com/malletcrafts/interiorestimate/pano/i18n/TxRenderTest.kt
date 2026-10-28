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
        // The Marathi was "{pl2} मध्ये {pl}", which printed "2 दिवस मध्ये 3 फोटो" (it wants दिवसांत, a noun
        // form the table does not have). Port 61c slice L2 finalised it as app.over_x's own review fix did,
        // with " · ", so the whole literal is pinned now: each count with its own noun, photos first.
        assertEquals("3 फोटो · 2 दिवस", Tx.MR.app_x_over_x(3, 2))
        assertEquals("1 फोटो · 2 दिवस", Tx.MR.app_x_over_x(1, 2))
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

    /**
     * THE 14 SPLIT TEMPLATES (the v61c card's group 2, landed in port 61c slice L2; design-marathi §3.3: "the
     * 14 split pairs, once they exist: each half with a literal Marathi value"). Each English word the
     * prototype's code chose (Hide / Show, on / off, Lock / Unlock, is / are, photo / 360, room / face) is
     * now a whole key, so the Marathi inflects it; the old templates stay refused (check 4). The four
     * pairs the card reuses (export.saved_x, sp.at_*_corner, sp.x_from_*_corner) were keyed before L2.
     */
    @Test fun `each split template renders each case as a whole sentence`() {
        val tick = Tx.EN.bundle_x_x_ticked_x(done = "2", of = "5", v = "Kitchen")
        assertEquals("Hide the five parts — 2 of 5 ticked for Kitchen", Tx.EN.bundle_aria_hide_five_parts_x(tick))
        assertEquals("Show the five parts", Tx.EN.bundle_aria_show_five_parts_x(null))
        assertEquals("पाचही भाग लपवा — किचन साठी 5 पैकी 2 टिक", Tx.MR.bundle_aria_hide_five_parts_x(Tx.MR.bundle_x_x_ticked_x(done = "2", of = "5", v = "किचन")))
        assertEquals("पाचही भाग दाखवा", Tx.MR.bundle_aria_show_five_parts_x(null))

        assertEquals("Saved ✓ plan.pdf · on the phone Android's Share sheet opens", Tx.EN.export_saved_x_share("plan.pdf"))
        assertEquals("सेव्ह ✓ plan.pdf · फोनवर Android चे शेअर पॅनेल उघडते", Tx.MR.export_saved_x_share("plan.pdf"))
        assertEquals("1 figure in this scope is ≈ from the photo, not measured", Tx.EN.export_x_figure_photo_not("1"))
        assertEquals("3 figures in this scope are ≈ from the photo, not measured", Tx.EN.export_x_figures_photo_not("3"))
        assertEquals("या व्याप्तीतील आकडा: 1 ≈ फोटोवरून, अजून मोजमाप नाही", Tx.MR.export_x_figure_photo_not("1"))
        assertEquals("या व्याप्तीतील आकडे: 3 ≈ फोटोवरून, अजून मोजमाप नाही", Tx.MR.export_x_figures_photo_not("3"))

        assertEquals("Phase 2 of 5: N", Tx.EN.phase_aria_phase_x_of_5_x(n = "2", name = "N"))
        assertEquals("Phase 2 of 5: N (now)", Tx.EN.phase_aria_phase_x_of_5_x_now(n = "2", name = "N"))
        assertEquals("फेज 2/5: N", Tx.MR.phase_aria_phase_x_of_5_x(n = "2", name = "N"))
        assertEquals("फेज 2/5: N (सध्या)", Tx.MR.phase_aria_phase_x_of_5_x_now(n = "2", name = "N"))

        assertEquals("Lock the floor's four lines", Tx.EN.sp_aria_lock_x_four_lines("floor"))
        assertEquals("Unlock the floor's four lines", Tx.EN.sp_aria_unlock_x_four_lines("floor"))
        assertEquals("जमीन: चार रेषा लॉक करा", Tx.MR.sp_aria_lock_x_four_lines("जमीन"))
        assertEquals("जमीन: चार रेषा अनलॉक करा", Tx.MR.sp_aria_unlock_x_four_lines("जमीन"))

        assertEquals("Locked 1 item · floor", Tx.EN.sp_msg_locked_x_item_x(n = 1, surface = "floor"))
        assertEquals("Unlocked 3 items · floor", Tx.EN.sp_msg_unlocked_x_item_x(n = 3, surface = "floor"))
        assertEquals("3 आयटम लॉक केले · जमीन", Tx.MR.sp_msg_locked_x_item_x(n = 3, surface = "जमीन"))
        assertEquals("1 आयटम अनलॉक केले · जमीन", Tx.MR.sp_msg_unlocked_x_item_x(n = 1, surface = "जमीन"))

        assertEquals("Office view: on", Tx.EN.sp_office_view_on)
        assertEquals("Office view: off", Tx.EN.sp_office_view_off)
        assertEquals("ऑफिस व्ह्यू: चालू", Tx.MR.sp_office_view_on)
        assertEquals("ऑफिस व्ह्यू: बंद", Tx.MR.sp_office_view_off)

        assertEquals("Placing Door · tap the photo", Tx.EN.sp_placing_x_tap_photo("Door"))
        assertEquals("Placing Door · tap the 360", Tx.EN.sp_placing_x_tap_360("Door"))
        assertEquals("दरवाजा ठेवत आहे · फोटोवर टॅप करा", Tx.MR.sp_placing_x_tap_photo("दरवाजा"))
        assertEquals("दरवाजा ठेवत आहे · 360 वर टॅप करा", Tx.MR.sp_placing_x_tap_360("दरवाजा"))

        assertEquals("Lock all tapes", Tx.EN.sp_lock_all_x(Tx.EN.sp_group_tapes))
        assertEquals("Unlock all chains", Tx.EN.sp_unlock_all_x(Tx.EN.sp_group_chains))
        assertEquals("सर्व टेप लॉक करा", Tx.MR.sp_lock_all_x(Tx.MR.sp_group_tapes))
        assertEquals("सर्व साखळ्या अनलॉक करा", Tx.MR.sp_unlock_all_x(Tx.MR.sp_group_chains))

        assertEquals("No E marks in this room", Tx.EN.t53_no_x_marks_room("E"))
        assertEquals("No E marks on this face", Tx.EN.t53_no_x_marks_face("E"))
        assertEquals("या खोलीत E मार्क नाहीत", Tx.MR.t53_no_x_marks_room("E"))
        assertEquals("या बाजूवर E मार्क नाहीत", Tx.MR.t53_no_x_marks_face("E"))

        assertEquals("1 wished mark is not on any ticked row — tick it? ", Tx.EN.wish_x_not_any_one(1))
        assertEquals("3 wished marks are not on any ticked row — tick it? ", Tx.EN.wish_x_not_any_many(3))
        assertEquals("1 wished mark is not on any ticked row — tick it? Electrical", Tx.EN.wish_x_not_any_one_svc(plN = 1, svc = "Electrical"))
        assertEquals("3 wished marks are not on any ticked row — tick it? Electrical", Tx.EN.wish_x_not_any_many_svc(plN = 3, svc = "Electrical"))
        assertEquals("1 इच्छित मार्क कोणत्याही टिक केलेल्या ओळीत नाही — टिक करायचे? ", Tx.MR.wish_x_not_any_one(1))
        assertEquals("3 इच्छित मार्क कोणत्याही टिक केलेल्या ओळीत नाहीत — टिक करायचे? ", Tx.MR.wish_x_not_any_many(3))
        assertEquals("1 इच्छित मार्क कोणत्याही टिक केलेल्या ओळीत नाही — टिक करायचे? विद्युत", Tx.MR.wish_x_not_any_one_svc(plN = 1, svc = "विद्युत"))
        assertEquals("3 इच्छित मार्क कोणत्याही टिक केलेल्या ओळीत नाहीत — टिक करायचे? विद्युत", Tx.MR.wish_x_not_any_many_svc(plN = 3, svc = "विद्युत"))
    }

    /**
     * The test above pins every split case in BOTH languages, and this holds it to that: an accessor it pins in
     * English only, or in Marathi only, fails here (L2 review: wish_x_not_any_one_svc had its English pin and no
     * Marathi one). It reads this file's own source, never the tables.
     */
    @Test fun `every split template pinned in English is pinned in Marathi too`() {
        val src = I18n.android.resolve("pano/src/test/kotlin/com/malletcrafts/interiorestimate/pano/i18n/TxRenderTest.kt").readText()
        val start = src.indexOf("fun `each split template renders each case as a whole sentence`")
        val end = if (start < 0) -1 else src.indexOf("@Test", start)
        assertTrue(start >= 0 && end > start, "the split-template test is where this check looks for it")
        val body = src.substring(start, end)
        fun pinned(lang: String) = Regex("""\bTx\.$lang\.(\w+)""").findAll(body).map { m -> m.groupValues[1] }.toSortedSet()
        val en = pinned("EN")
        val mr = pinned("MR")
        assertTrue(en.size >= 23, "the split-template test pins the 23 split keys; found ${en.size} accessors")
        assertEquals(emptySet<String>(), en - mr, "pinned in English and not in Marathi")
        assertEquals(emptySet<String>(), mr - en, "pinned in Marathi and not in English")
    }

    @Test fun `a phone text's count folds its plural suffix, and the Marathi drops it`() {
        // the v61c card's group 1 (L2): {n} with {pl} = n===1?'':'s' is ONE Int parameter
        assertEquals("1 waiting", Tx.EN.app_x_waiting_queue(1))
        // "3 waitings" is the prototype's plural(unsent(),'waiting') and the phone's subtitle byte for byte
        // (MainActivity.kt:1119-1124): the key keeps it, so a slice that calls it changes nothing on screen
        assertEquals("3 waitings", Tx.EN.app_x_waiting_queue(3))
        assertEquals("3 बाकी", Tx.MR.app_x_waiting_queue(3))
        assertEquals("Sent 1 annotated face to MEST-PH-1", Tx.EN.app_msg_sent_x_annotated_to_x(n = 1, server = "MEST-PH-1"))
        assertEquals("Sent 3 annotated faces to MEST-PH-1", Tx.EN.app_msg_sent_x_annotated_to_x(n = 3, server = "MEST-PH-1"))
        assertEquals("1 पॉइंट मध्यावर आणले", Tx.MR.sp_file_x_points_recentred(1))
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
