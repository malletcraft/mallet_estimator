package com.malletcrafts.interiorestimate.pano

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * THE CAPTURE STORE STAYS REVERT-SAFE (port 61c slices D0 and D1, 28 Oct 2026).
 *
 * CaptureStore's onDowngrade does nothing, so a build that reverts a schema change -- a NEWER versionCode carrying
 * OLDER schema code -- still opens the offline queue on the phone that may be holding unsent captures. Doing nothing
 * is safe only while the rules in its KDoc hold, and until D0's review the KDoc was the only thing holding them: "a
 * hazard written down in a comment is not a hazard handled". Since D1 the upgrade statements are [CaptureSchema]'s,
 * here in :pano, and this test reads them as values; :app has no unit tests and cannot compile here, so the rest is
 * read from CaptureStore.kt beside :pano (as RefFrameTest reads the app). It refuses, each by name:
 *
 *  - no onDowngrade, or one that does anything (SQLiteOpenHelper's own throws);
 *  - a step that is not `ALTER TABLE captures ADD COLUMN`; an onUpgrade that runs anything but the steps of
 *    `for (sql in CaptureSchema.upgrade(old))`, SQL of its own, or any other db call;
 *  - a step run outside a runCatching, in one that rethrows, or in ONE runCatching around the whole loop: after a
 *    downgrade the helper writes the older version back, so the next newer build re-runs its steps over columns that
 *    exist, and the duplicate column is the expected failure there -- one that skipped the steps after it would leave
 *    their columns missing for good;
 *  - a step SQLite refuses on ADD COLUMN -- NOT NULL with no DEFAULT or DEFAULT NULL, a DEFAULT that is not a
 *    constant, PRIMARY KEY or UNIQUE. Inside runCatching that refusal is SWALLOWED, so the column would simply be
 *    missing on every upgraded phone, with no error, and only a fresh install would have it;
 *  - a step for a version that never runs (1, which is onCreate's, or past the helper's version), steps out of
 *    order, a helper version that is not the newest step's (a version raised with no step marks every phone as
 *    already having the next real one), or a helper that does not take CaptureSchema.VERSION;
 *  - an added column whose shape in onCreate differs from its ADD COLUMN (an upgraded phone and a fresh install
 *    would then hold different tables), a column in onCreate that no step adds and version 1 did not create, or a
 *    version-1 column gone from onCreate (a drop or a rename);
 *  - a row read by position, or a positional INSERT: a column this build does not know must never be read or moved;
 *  - the work column read without its NULL: getInt reads NULL as 0, the quote journey, and NULL is "filed by its
 *    stage", every capture not yet filed by work (every row from before version 6 among them).
 *
 * The third test proves the reader sees every one of those on the real file and the real steps with one thing
 * changed, so it cannot pass by reading nothing.
 */
class CaptureStoreRevertTest {

    private val file = File("../app/src/main/kotlin/com/malletcrafts/interiorestimate/CaptureStore.kt")

    private fun source(): String {
        assertTrue(file.isFile, "the app's CaptureStore.kt is beside :pano (${file.absolutePath})")
        return file.readText()
    }

    /** What the reader reads: CaptureStore.kt's text, and CaptureSchema's version and steps. */
    private data class Input(val src: String, val version: Int, val steps: List<CaptureSchema.Step>)

    private fun real() = Input(source(), CaptureSchema.VERSION, CaptureSchema.STEPS)

    /** One ADD COLUMN: the version that added it, the column, and its definition with spaces collapsed. */
    private data class Add(val since: Int, val column: String, val def: String)

    private class Reading(val adds: List<Add>, val offences: List<String>)

    /** The columns version 1 created. No step adds them, and this list never grows: a new column is a step, never a
     *  line added here. Every other column in onCreate is one a step adds. */
    private val V1 = setOf("device_id", "project", "project_title", "customer_name", "room", "stage", "capture_date",
        "pano_path", "created_at", "state", "server_name", "error")

    @Test fun `the capture store opens a newer database and its schema only ever grows`() {
        val r = read(real())
        assertEquals(emptyList(), r.offences)
        assertEquals(6, CaptureSchema.VERSION, "the helper's schema version")
        // A pin, so the rules above cannot pass by finding no statement at all. A new version adds its row here.
        assertEquals(listOf(
            Add(2, "sku", "TEXT NOT NULL DEFAULT ''"),
            Add(3, "kind", "TEXT NOT NULL DEFAULT '360'"),
            Add(4, "fov", "REAL NOT NULL DEFAULT 0"),
            Add(5, "room_l_in", "REAL NOT NULL DEFAULT 0"),
            Add(5, "room_w_in", "REAL NOT NULL DEFAULT 0"),
            Add(5, "room_h_in", "REAL NOT NULL DEFAULT 0"),
            Add(6, "work", "INTEGER"),
        ), r.adds)
    }

    @Test fun `a database at each version runs exactly the steps it has not had`() {
        val sku = "ALTER TABLE captures ADD COLUMN sku TEXT NOT NULL DEFAULT ''"
        val kind = "ALTER TABLE captures ADD COLUMN kind TEXT NOT NULL DEFAULT '360'"
        val fov = "ALTER TABLE captures ADD COLUMN fov REAL NOT NULL DEFAULT 0"
        val room = listOf("ALTER TABLE captures ADD COLUMN room_l_in REAL NOT NULL DEFAULT 0",
            "ALTER TABLE captures ADD COLUMN room_w_in REAL NOT NULL DEFAULT 0",
            "ALTER TABLE captures ADD COLUMN room_h_in REAL NOT NULL DEFAULT 0")
        val work = "ALTER TABLE captures ADD COLUMN work INTEGER"
        assertEquals(listOf(sku, kind, fov) + room + work, CaptureSchema.upgrade(1), "a version-1 phone runs every step")
        assertEquals(listOf(fov) + room + work, CaptureSchema.upgrade(3))
        assertEquals(room + work, CaptureSchema.upgrade(4))
        // Every phone on 0.3.262 is at 5, and runs this one statement; so does a phone a reverted build wrote 5 back
        // on, over a column it already has.
        assertEquals(listOf(work), CaptureSchema.upgrade(5), "version 5 to 6 adds exactly `work INTEGER`")
        assertEquals(emptyList(), CaptureSchema.upgrade(6), "a current database runs nothing")
    }

    @Test fun `the reader sees every way the rule can be broken`() {
        val real = real()
        val v = real.version
        fun src(change: (String) -> String): (Input) -> Input = { it.copy(src = change(it.src)) }
        fun steps(change: (List<CaptureSchema.Step>) -> List<CaptureSchema.Step>): (Input) -> Input =
            { it.copy(steps = change(it.steps)) }
        fun swap(from: String, to: String) = steps { l -> l.map { if (from in it.sql) it.copy(sql = it.sql.replace(from, to)) else it } }
        fun add(since: Int, sql: String) = steps { it + CaptureSchema.Step(since, sql) }
        val loop = "for (sql in CaptureSchema.upgrade(old)) runCatching { db.execSQL(sql) }"
        val readWork = "ix(\"work\").let { i -> if (cur.isNull(i)) null else cur.getInt(i) }"
        val probes: List<Triple<String, (Input) -> Input, String>> = listOf(
            // onDowngrade
            Triple("the override deleted", src { it.replace("override fun onDowngrade(db: SQLiteDatabase, old: Int, new: Int) {}", "") },
                "no onDowngrade"),
            Triple("the override commented out", src { it.replace("override fun onDowngrade(", "// override fun onDowngrade(") },
                "no onDowngrade"),
            Triple("the default behaviour called", src { it.replace("new: Int) {}", "new: Int) { super.onDowngrade(db, old, new) }") },
                "onDowngrade does something"),
            Triple("an expression body", src { it.replace("new: Int) {}", "new: Int) = Unit") },
                "onDowngrade is not an empty block"),
            // onUpgrade runs CaptureSchema's steps and nothing else
            Triple("a rebuild in onUpgrade", src { it.replace(loop, "db.execSQL(\"DROP TABLE captures\")\n        $loop") },
                "of its own"),
            Triple("another db call in onUpgrade", src { it.replace(loop, "db.delete(\"captures\", null, null)\n        $loop") },
                "db.delete"),
            Triple("a step outside runCatching", src { it.replace("runCatching { db.execSQL(sql) }", "db.execSQL(sql)") },
                "outside a runCatching"),
            Triple("one runCatching around every step", src { it.replace(loop, "runCatching { for (sql in CaptureSchema.upgrade(old)) db.execSQL(sql) }") },
                "ONE runCatching"),
            Triple("a runCatching that rethrows", src { it.replace("runCatching { db.execSQL(sql) }", "runCatching { db.execSQL(sql) }.getOrThrow()") },
                "rethrows"),
            Triple("the steps of the wrong version", src { it.replace("CaptureSchema.upgrade(old)", "CaptureSchema.upgrade(new)") },
                "not a step of"),
            Triple("the steps never run", src { it.replace(loop, "") },
                "runs no CaptureSchema step"),
            Triple("the helper's version written out", src { it.replace("null, CaptureSchema.VERSION)", "null, $v)") },
                "does not take CaptureSchema.VERSION"),
            // the steps, as values
            Triple("a rebuild step", add(v, "DROP TABLE captures"),
                "not an ADD COLUMN"),
            Triple("NOT NULL with no DEFAULT", swap("ADD COLUMN sku TEXT NOT NULL DEFAULT ''", "ADD COLUMN sku TEXT NOT NULL"),
                "NOT NULL with no DEFAULT"),
            Triple("a DEFAULT that is not a constant", swap("ADD COLUMN fov REAL NOT NULL DEFAULT 0", "ADD COLUMN fov REAL NOT NULL DEFAULT (1.0 * 0)"),
                "not a constant"),
            Triple("a UNIQUE column", swap("ADD COLUMN fov REAL NOT NULL DEFAULT 0", "ADD COLUMN fov REAL UNIQUE"),
                "PRIMARY KEY or UNIQUE"),
            Triple("a step past the helper's version", add(v + 1, "ALTER TABLE captures ADD COLUMN probe_col INTEGER"),
                "past the helper's version $v"),
            Triple("a step for version 1", steps { listOf(CaptureSchema.Step(1, "ALTER TABLE captures ADD COLUMN error TEXT")) + it },
                "version 1 is onCreate's"),
            Triple("a version raised with no step", { i: Input -> i.copy(version = v + 1) },
                "not the newest step's"),
            Triple("steps out of order", steps { it.takeLast(1) + it.dropLast(1) },
                "out of order"),
            Triple("a step's column onCreate does not have", add(v, "ALTER TABLE captures ADD COLUMN room_d_in REAL NOT NULL DEFAULT 0"),
                "room_d_in is added but onCreate does not create it"),
            Triple("a column added twice in two shapes", add(v, "ALTER TABLE captures ADD COLUMN work TEXT"),
                "work is added twice"),
            // onCreate gives every column the shape its step does
            Triple("onCreate's shape differs from the ADD COLUMN", src { it.replace("kind TEXT NOT NULL DEFAULT '360',", "kind TEXT NOT NULL DEFAULT 'Photo',") },
                "onCreate gives kind"),
            Triple("a column in onCreate no step adds", src { it.replace("room TEXT NOT NULL,", "room TEXT NOT NULL,\n                 probe_col INTEGER,") },
                "probe_col is in onCreate but no ADD COLUMN adds it"),
            Triple("the work column missing from onCreate", src { it.replace("room_h_in REAL NOT NULL DEFAULT 0,\n                 work INTEGER", "room_h_in REAL NOT NULL DEFAULT 0") },
                "work is added but onCreate does not create it"),
            Triple("a version-1 column dropped", src { it.replace("error TEXT,", "") },
                "version-1 column error is gone"),
            // rows read and written by name, and work read as null when NULL
            Triple("a row read by position", src { it.replace("cur.getString(ix(\"room\"))", "cur.getString(4)") },
                "by position"),
            Triple("a positional insert", src { it.replace("    fun all(): List<Capture>",
                "    fun bad() = writableDatabase.execSQL(\"INSERT INTO captures VALUES ('a')\")\n    fun all(): List<Capture>") },
                "positional INSERT"),
            Triple("work read without its NULL", src { it.replace(readWork, "cur.getInt(ix(\"work\"))") },
                "work is read without isNull"),
        )
        for ((why, change, said) in probes) {
            val bad = change(real)
            assertNotEquals(real, bad, "$why: the probe changed nothing in CaptureStore.kt or the steps, so it proves nothing")
            val off = read(bad).offences
            assertTrue(off.any { said in it }, "$why: expected an offence saying '$said', got $off")
        }
    }

    // ---- the reader ----------------------------------------------------------------------------------------------

    private fun read(input: Input): Reading {
        val code = stripComments(input.src)
        val version = input.version
        val off = ArrayList<String>()
        if (!Regex("""SQLiteOpenHelper\(\s*\w+\s*,\s*"captures\.db"\s*,\s*null\s*,\s*CaptureSchema\s*\.\s*VERSION\s*\)""").containsMatchIn(code))
            off += "the helper does not take CaptureSchema.VERSION: SQLiteOpenHelper(context, \"captures.db\", null, CaptureSchema.VERSION)"

        // onDowngrade: present, a block, and empty
        val down = bodyOf(code, "onDowngrade")
        if (down == null) off += "no onDowngrade: SQLiteOpenHelper's default throws, and a reverted build cannot open its queue"
        else if (!down.block) off += "onDowngrade is not an empty block body"
        else if (down.text(code).isNotBlank())
            off += "onDowngrade does something (${down.text(code).trim().take(60)}); it must leave the database as it is"

        // the steps: each an additive ADD COLUMN, for a version that runs, in order
        val adds = ArrayList<Add>()
        val newest = input.steps.maxOfOrNull { it.since }
        if (newest != version) off += "the helper's version $version is not the newest step's ($newest): a phone marked $version " +
            "would never run a later step written for it"
        input.steps.zipWithNext().filter { (a, b) -> b.since < a.since }.forEach { (a, b) ->
            off += "steps out of order: version ${b.since}'s '${b.sql.take(50)}' after version ${a.since}'s" }
        for (st in input.steps) {
            val where = "CaptureSchema step '${st.sql.trim().take(70)}'"
            if (st.since < 2) off += "$where is for version ${st.since}, which never runs: version 1 is onCreate's"
            else if (st.since > version) off += "$where is for version ${st.since}, past the helper's version $version, so it never runs"
            val m = Regex("""^\s*ALTER\s+TABLE\s+captures\s+ADD\s+COLUMN\s+(\w+)\s+(\S.*?)\s*;?\s*$""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(st.sql)
            if (m == null) { off += "$where is not an ADD COLUMN on captures; a schema change here is additive only"; continue }
            val (col, raw) = m.destructured
            val def = norm(raw)
            off += columnRules(col, def)
            adds += Add(st.since, col, def)
        }

        // onUpgrade: the steps of CaptureSchema.upgrade(old), each in a runCatching, and nothing else
        val sig = Regex("""\boverride\s+fun\s+onUpgrade\s*\(\s*(\w+)\s*:\s*SQLiteDatabase\s*,\s*(\w+)\s*:\s*Int\s*,\s*\w+\s*:\s*Int\s*\)""").find(code)
        val up = bodyOf(code, "onUpgrade")
        if (sig == null || up == null || !up.block) off += "no onUpgrade(db: SQLiteDatabase, old: Int, new: Int) block"
        else {
            val (db, old) = sig.destructured
            Regex("""\b$db\s*\.\s*(\w+)""").findAll(up.text(code)).map { it.groupValues[1] }
                .filter { it != "execSQL" }.forEach { off += "onUpgrade calls $db.$it; an upgrade only ever runs a CaptureSchema step" }
            var runs = 0
            for (st in statements(code, up)) {
                val where = "onUpgrade's execSQL(${st.arg.take(70)})"
                if (st.literal != null) { off += "onUpgrade runs '${st.literal.trim().take(70)}' of its own: every upgrade statement is a CaptureSchema step, where this test reads it"; continue }
                // the loop's header sits in an enclosing block's opener, or (a loop with no braces) in this statement's prefix
                val ctx = st.openers.map { it.text } + st.prefix
                val loop = Regex("""\bfor\s*\(\s*(\w+)\s+in\s+CaptureSchema\s*\.\s*upgrade\s*\(\s*$old\s*\)\s*\)""")
                val loopAt = ctx.indexOfFirst { loop.containsMatchIn(it) }
                val each = if (loopAt < 0) null else loop.find(ctx[loopAt])?.groupValues?.get(1)
                if (each == null || st.arg != each) { off += "$where is not a step of `for (sql in CaptureSchema.upgrade($old))`"; continue }
                runs++
                val inner = st.openers.lastOrNull()
                if (inner == null || !Regex("""\brunCatching$""").containsMatchIn(inner.text))
                    off += "$where is outside a runCatching: a down-then-up cycle re-runs it over a column that exists"
                else if (loopAt == ctx.size - 1)
                    off += "$where runs every step in ONE runCatching: the first column that exists would skip every step after it"
                else if (code.substring(close(code, inner.at) + 1).trimStart().startsWith(".getOrThrow"))
                    off += "$where sits in a runCatching that rethrows"
            }
            if (runs == 0) off += "onUpgrade runs no CaptureSchema step: every upgraded phone would lack every new column"
        }

        // onCreate: the same shape as every ADD COLUMN, and nothing an upgraded phone would lack
        val created = createdColumns(code, off)
        if (created != null) {
            val added = LinkedHashMap<String, String>()
            for (a in adds) {
                val was = added.put(a.column, a.def)
                if (was != null && was != a.def) off += "${a.column} is added twice, as '$was' and '${a.def}'"
                val made = created[a.column]
                if (made == null) off += "${a.column} is added but onCreate does not create it: a fresh install would lack it"
                else if (made != a.def) off += "onCreate gives ${a.column} '$made' but its ADD COLUMN gives '${a.def}': " +
                    "an upgraded phone and a fresh install would hold different tables"
            }
            for (c in created.keys) if (c !in V1 && c !in added)
                off += "$c is in onCreate but no ADD COLUMN adds it: every upgraded phone would lack it"
            for (c in V1) if (c !in created) off += "version-1 column $c is gone from onCreate: a drop or a rename is not additive"
        }

        // rows read and written by name
        Regex("""\.get(?:String|Long|Double|Int|Float|Short|Blob)\(\s*\d""").findAll(code).forEach {
            off += "a cursor column read by position at line ${lineOf(code, it.range.first)}: read it by name" }
        if (!code.contains("getColumnIndexOrThrow(")) off += "rows are not read through getColumnIndexOrThrow"
        Regex("""(?:INSERT(?:\s+OR\s+\w+)?|REPLACE)\s+INTO\s+captures\s+(?:VALUES|SELECT)""", RegexOption.IGNORE_CASE)
            .findAll(code).forEach { off += "a positional INSERT at line ${lineOf(code, it.range.first)}: name every column" }

        // work: NULL is "filed by its stage", and getInt would read it as 0, the quote journey
        val workReads = Regex("""\b(?:ix|getColumnIndexOrThrow|getColumnIndex)\(\s*"work"\s*\)""").findAll(code).toList()
        if (workReads.isEmpty()) off += "the work column is not read by name"
        workReads.forEach {
            if (!lineText(code, it.range.first).contains("isNull(")) off += "work is read without isNull at line " +
                "${lineOf(code, it.range.first)}: a NULL would read as 0, the quote journey, for every capture not yet filed by work"
        }
        return Reading(adds, off)
    }

    /** What SQLite refuses on ADD COLUMN, which runCatching would swallow, leaving the column missing. */
    private fun columnRules(col: String, def: String): List<String> {
        val off = ArrayList<String>()
        val bare = def.replace(Regex("'(?:[^']|'')*'"), "''").uppercase()
        if (!Regex("""^(TEXT|INTEGER|REAL|BLOB|NUMERIC)\b""").containsMatchIn(bare)) off += "ADD COLUMN $col has no plain type"
        if (Regex("""\bPRIMARY\s+KEY\b|\bUNIQUE\b""").containsMatchIn(bare)) off += "ADD COLUMN $col is PRIMARY KEY or UNIQUE, which SQLite refuses"
        val dflt = Regex("""\bDEFAULT\s+('(?:[^']|'')*'|\S+)""", RegexOption.IGNORE_CASE).find(def)?.groupValues?.get(1)
        if (dflt != null && !Regex("""^(?:'(?:[^']|'')*'|[-+]?\d+(?:\.\d+)?|NULL)$""", RegexOption.IGNORE_CASE).matches(dflt))
            off += "ADD COLUMN $col has DEFAULT $dflt, which is not a constant; SQLite refuses it"
        if (Regex("""\bNOT\s+NULL\b""").containsMatchIn(bare) && (dflt == null || dflt.equals("NULL", ignoreCase = true)))
            off += "ADD COLUMN $col is NOT NULL with no DEFAULT, which SQLite refuses"
        return off
    }

    /** The columns of onCreate's `CREATE TABLE captures (...)`, name to definition. */
    private fun createdColumns(code: String, off: MutableList<String>): Map<String, String>? {
        val b = bodyOf(code, "onCreate")
        if (b == null || !b.block) { off += "no onCreate block"; return null }
        val create = statements(code, b).firstNotNullOfOrNull { st ->
            st.literal?.let {
                Regex("""^\s*CREATE\s+TABLE\s+captures\s*\((.*)\)\s*$""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                    .find(it)?.groupValues?.get(1) } }
        if (create == null) { off += "onCreate has no CREATE TABLE captures literal"; return null }
        val out = LinkedHashMap<String, String>()
        for (part in splitTop(create)) {
            val m = Regex("""^\s*(\w+)\s+(\S.*?)\s*$""", RegexOption.DOT_MATCHES_ALL).find(part) ?: continue
            val (name, raw) = m.destructured
            if (name.uppercase() in setOf("PRIMARY", "UNIQUE", "CHECK", "FOREIGN", "CONSTRAINT")) continue
            out[name] = norm(raw)
        }
        return out
    }

    private fun norm(s: String) = s.replace(Regex("\\s+"), " ").trim()

    /** Commas at the top level of a column list: not in parentheses, not in a quoted SQL string. */
    private fun splitTop(s: String): List<String> {
        val out = ArrayList<String>(); var depth = 0; var q = false; var from = 0
        for ((i, c) in s.withIndex()) when {
            c == '\'' -> q = !q
            q -> {}
            c == '(' -> depth++
            c == ')' -> depth--
            c == ',' && depth == 0 -> { out += s.substring(from, i); from = i + 1 }
        }
        out += s.substring(from)
        return out
    }

    // ---- a small Kotlin reader: comments, literals, blocks --------------------------------------------------------

    private class Opener(val text: String, val at: Int)
    /** A function's body: the code between its braces, [from] until [to]; [block] false for an expression body. */
    private class Body(val from: Int, val to: Int, val block: Boolean) { fun text(code: String) = code.substring(from, to) }
    /** One `execSQL(...)`: its argument as written, the SQL when that argument is one string literal, the blocks it sits
     *  in, outermost first, and the code before it in the innermost one ([prefix]: where a loop with no braces is). */
    private class Stmt(val arg: String, val literal: String?, val openers: List<Opener>, val prefix: String)

    /** The source with every comment blanked to spaces, newlines kept so line numbers hold; strings untouched. */
    private fun stripComments(s: String): String {
        val out = StringBuilder(s)
        fun blank(from: Int, to: Int) { for (k in from until to) if (out[k] != '\n') out[k] = ' ' }
        var i = 0
        while (i < s.length) {
            val j = skipLiteral(s, i)
            if (j != i) { i = j; continue }
            if (s.startsWith("//", i)) {
                val e = s.indexOf('\n', i).let { if (it < 0) s.length else it }; blank(i, e); i = e
            } else if (s.startsWith("/*", i)) {
                var depth = 0; var k = i
                while (k < s.length) {
                    if (s.startsWith("/*", k)) { depth++; k += 2 }
                    else if (s.startsWith("*/", k)) { depth--; k += 2; if (depth == 0) break }
                    else k++
                }
                blank(i, k); i = k
            } else i++
        }
        return out.toString()
    }

    /** Past the string or char literal starting at [i], or [i] itself when none starts there. */
    private fun skipLiteral(s: String, i: Int): Int = when {
        s.startsWith("\"\"\"", i) -> s.indexOf("\"\"\"", i + 3).let { if (it < 0) s.length else it + 3 }
        s[i] == '"' -> {
            var k = i + 1
            while (k < s.length && s[k] != '"' && s[k] != '\n') k += if (s[k] == '\\') 2 else 1
            minOf(k + 1, s.length)
        }
        s[i] == '\'' -> {
            val k = i + 1 + (if (i + 1 < s.length && s[i + 1] == '\\') 2 else 1)
            val e = s.indexOf('\'', minOf(k, s.length))
            if (e in k..k + 6) e + 1 else i + 1
        }
        else -> i
    }

    /** The [shut] closing the [open] at [at] (a `{` by default), string and char literals skipped. */
    private fun close(code: String, at: Int, open: Char = '{', shut: Char = '}'): Int {
        var depth = 0; var i = at
        while (i < code.length) {
            val j = skipLiteral(code, i)
            if (j != i) { i = j; continue }
            when (code[i]) { open -> depth++; shut -> { depth--; if (depth == 0) return i } }
            i++
        }
        error("unbalanced '$open' from offset $at")
    }

    /** The inside of `override fun [name](...) { ... }`: null when there is no such function. */
    private fun bodyOf(code: String, name: String): Body? {
        val m = Regex("""\boverride\s+fun\s+$name\s*\(""").find(code) ?: return null
        val i = close(code, m.range.last, '(', ')')
        var k = i + 1
        while (k < code.length && code[k].isWhitespace()) k++
        if (k >= code.length || code[k] != '{') return Body(k, k, block = false)
        return Body(k + 1, close(code, k), block = true)
    }

    /** The body of the string literal that is the whole of [arg], or null when [arg] is anything else. */
    private fun literalOf(arg: String): String? {
        if (arg.isEmpty() || arg[0] != '"' || skipLiteral(arg, 0) != arg.length) return null
        return if (arg.startsWith("\"\"\"")) arg.substring(3, arg.length - 3) else arg.substring(1, arg.length - 1)
    }

    /** Every `execSQL(` in [body], with its argument and the blocks it sits in, outermost first. */
    private fun statements(code: String, body: Body): List<Stmt> {
        val out = ArrayList<Stmt>(); val stack = ArrayList<Opener>()
        var boundary = body.from; var i = body.from
        while (i < body.to) {
            val j = skipLiteral(code, i)
            if (j != i) { i = j; continue }
            val c = code[i]
            if (c == '{') { stack += Opener(code.substring(boundary, i).trim(), i); boundary = i + 1 }
            else if (c == '}') { if (stack.isNotEmpty()) stack.removeAt(stack.size - 1); boundary = i + 1 }
            else if (code.startsWith("execSQL(", i) && (i == 0 || !code[i - 1].isLetterOrDigit())) {
                val open = i + "execSQL".length
                val end = close(code, open, '(', ')')
                val arg = code.substring(open + 1, end).trim()
                out += Stmt(arg, literalOf(arg), stack.toList(), code.substring(boundary, i).trim())
                i = end + 1; continue
            }
            i++
        }
        return out
    }

    private fun lineOf(s: String, at: Int) = s.substring(0, at).count { it == '\n' } + 1

    private fun lineText(s: String, at: Int): String {
        val from = s.lastIndexOf('\n', at - 1) + 1
        return s.substring(from, s.indexOf('\n', at).let { if (it < 0) s.length else it })
    }
}
