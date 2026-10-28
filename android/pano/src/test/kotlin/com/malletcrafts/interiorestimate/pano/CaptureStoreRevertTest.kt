package com.malletcrafts.interiorestimate.pano

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * THE CAPTURE STORE STAYS REVERT-SAFE (port 61c slice D0, and the review of it, 28 Oct 2026).
 *
 * CaptureStore's onDowngrade does nothing, so a build that reverts a schema change -- a NEWER versionCode carrying
 * OLDER schema code -- still opens the offline queue on the phone that may be holding unsent captures. Doing nothing
 * is safe only while the rules in its KDoc hold, and until this test the KDoc was the only thing holding them: "a
 * hazard written down in a comment is not a hazard handled". :app has no unit tests and cannot compile here, so this
 * reads CaptureStore.kt from beside :pano (as RefFrameTest reads the app) and refuses, each by name:
 *
 *  - no onDowngrade, or one that does anything (SQLiteOpenHelper's own throws);
 *  - an upgrade statement that is not `ALTER TABLE captures ADD COLUMN`, or any other db call in onUpgrade;
 *  - one outside a runCatching, or in one that rethrows: after a downgrade the helper writes the older version back,
 *    so the next newer build re-runs its ALTERs over columns that exist, and the duplicate column is the expected
 *    failure there;
 *  - one SQLite refuses on ADD COLUMN -- NOT NULL with no DEFAULT or DEFAULT NULL, a DEFAULT that is not a constant,
 *    PRIMARY KEY or UNIQUE. Inside runCatching that refusal is SWALLOWED, so the column would simply be missing on
 *    every upgraded phone, with no error, and only a fresh install would have it;
 *  - one under `if (old < N)` past the helper's version, which never runs;
 *  - an added column whose shape in onCreate differs from its ADD COLUMN (an upgraded phone and a fresh install
 *    would then hold different tables), a column in onCreate that no ADD COLUMN adds and version 1 did not create,
 *    or a version-1 column gone from onCreate (a drop or a rename);
 *  - a row read by position, or a positional INSERT: a column this build does not know must never be read or moved.
 *
 * The second test proves the reader sees every one of those on the real file with one thing changed, so it cannot
 * pass by reading nothing. When the upgrade statements move out of CaptureStore.kt (D1 plans a :pano CaptureSchema
 * object), this reader moves with them and keeps every rule and every probe.
 */
class CaptureStoreRevertTest {

    private val file = File("../app/src/main/kotlin/com/malletcrafts/interiorestimate/CaptureStore.kt")

    private fun source(): String {
        assertTrue(file.isFile, "the app's CaptureStore.kt is beside :pano (${file.absolutePath})")
        return file.readText()
    }

    /** One ADD COLUMN: the `if (old < N)` it runs under, the column, and its definition with spaces collapsed. */
    private data class Add(val since: Int, val column: String, val def: String)

    private class Reading(val version: Int?, val adds: List<Add>, val offences: List<String>)

    /** The columns version 1 created. No ADD COLUMN adds them, and this list never grows: a new column is an ADD
     *  COLUMN, never a line added here. Every other column in onCreate is one an upgrade adds. */
    private val V1 = setOf("device_id", "project", "project_title", "customer_name", "room", "stage", "capture_date",
        "pano_path", "created_at", "state", "server_name", "error")

    @Test fun `the capture store opens a newer database and its schema only ever grows`() {
        val r = read(source())
        assertEquals(emptyList(), r.offences)
        assertEquals(5, r.version, "the helper's schema version")
        // A pin, so the rules above cannot pass by finding no statement at all. A new version adds its row here.
        assertEquals(listOf(
            Add(2, "sku", "TEXT NOT NULL DEFAULT ''"),
            Add(3, "kind", "TEXT NOT NULL DEFAULT '360'"),
            Add(4, "fov", "REAL NOT NULL DEFAULT 0"),
            Add(5, "room_l_in", "REAL NOT NULL DEFAULT 0"),
            Add(5, "room_w_in", "REAL NOT NULL DEFAULT 0"),
            Add(5, "room_h_in", "REAL NOT NULL DEFAULT 0"),
        ), r.adds)
    }

    @Test fun `the reader sees every way the rule can be broken`() {
        val real = source()
        val v = read(real).version ?: error("the helper's version is not found")
        val probes: List<Triple<String, (String) -> String, String>> = listOf(
            Triple("the override deleted", { s: String ->
                s.replace("override fun onDowngrade(db: SQLiteDatabase, old: Int, new: Int) {}", "") },
                "no onDowngrade"),
            Triple("the override commented out", { s: String ->
                s.replace("override fun onDowngrade(", "// override fun onDowngrade(") },
                "no onDowngrade"),
            Triple("the default behaviour called", { s: String ->
                s.replace("new: Int) {}", "new: Int) { super.onDowngrade(db, old, new) }") },
                "onDowngrade does something"),
            Triple("an expression body", { s: String -> s.replace("new: Int) {}", "new: Int) = Unit") },
                "onDowngrade is not an empty block"),
            Triple("a rebuild", { s: String ->
                s.replace("if (old < 5) {", "if (old < 5) {\n            db.execSQL(\"DROP TABLE captures\")") },
                "not an ADD COLUMN"),
            Triple("another db call in onUpgrade", { s: String ->
                s.replace("if (old < 5) {", "if (old < 5) {\n            db.delete(\"captures\", null, null)") },
                "db.delete"),
            Triple("an ALTER outside runCatching", { s: String ->
                Regex("""runCatching \{\s*(db\.execSQL\("ALTER TABLE captures ADD COLUMN kind[^\n]*\))\s*\}""")
                    .replace(s, "$1") },
                "outside a runCatching"),
            Triple("a runCatching that rethrows", { s: String ->
                Regex("""(ADD COLUMN kind[^\n]*\n\s*\})""").replace(s, "$1.getOrThrow()") },
                "rethrows"),
            Triple("NOT NULL with no DEFAULT", { s: String ->
                s.replace("ADD COLUMN sku TEXT NOT NULL DEFAULT ''", "ADD COLUMN sku TEXT NOT NULL") },
                "NOT NULL with no DEFAULT"),
            Triple("a DEFAULT that is not a constant", { s: String ->
                s.replace("ADD COLUMN fov REAL NOT NULL DEFAULT 0", "ADD COLUMN fov REAL NOT NULL DEFAULT (1.0 * 0)") },
                "not a constant"),
            Triple("a UNIQUE column", { s: String ->
                s.replace("ADD COLUMN fov REAL NOT NULL DEFAULT 0", "ADD COLUMN fov REAL UNIQUE") },
                "PRIMARY KEY or UNIQUE"),
            Triple("an upgrade past the helper's version", { s: String -> s.replace("if (old < $v)", "if (old < ${v + 1})") },
                "past the helper's version $v"),
            Triple("onCreate's shape differs from the ADD COLUMN", { s: String ->
                s.replace("kind TEXT NOT NULL DEFAULT '360',", "kind TEXT NOT NULL DEFAULT 'Photo',") },
                "onCreate gives kind"),
            Triple("a column in onCreate no upgrade adds", { s: String ->
                s.replace("room TEXT NOT NULL,", "room TEXT NOT NULL,\n                 probe_col INTEGER,") },
                "probe_col is in onCreate but no ADD COLUMN adds it"),
            Triple("a looped column onCreate does not have", { s: String ->
                s.replaceFirst("\"room_h_in\")", "\"room_h_in\", \"room_d_in\")") },
                "room_d_in is added but onCreate does not create it"),
            Triple("a version-1 column dropped", { s: String -> s.replace("error TEXT,", "") },
                "version-1 column error is gone"),
            Triple("a row read by position", { s: String ->
                s.replace("cur.getString(ix(\"room\"))", "cur.getString(4)") },
                "by position"),
            Triple("a positional insert", { s: String ->
                s.replace("    fun all(): List<Capture>",
                    "    fun bad() = writableDatabase.execSQL(\"INSERT INTO captures VALUES ('a')\")\n    fun all(): List<Capture>") },
                "positional INSERT"),
        )
        for ((why, change, said) in probes) {
            val bad = change(real)
            assertNotEquals(real, bad, "$why: the probe changed nothing in CaptureStore.kt, so it proves nothing")
            val off = read(bad).offences
            assertTrue(off.any { said in it }, "$why: expected an offence saying '$said', got $off")
        }
    }

    // ---- the reader ----------------------------------------------------------------------------------------------

    private fun read(src: String): Reading {
        val code = stripComments(src)
        val off = ArrayList<String>()
        val version = Regex("""SQLiteOpenHelper\(\s*\w+\s*,\s*"captures\.db"\s*,\s*null\s*,\s*(\d+)\s*\)""")
            .find(code)?.groupValues?.get(1)?.toInt()
        if (version == null) off += "the SQLiteOpenHelper(context, \"captures.db\", null, N) call is not found"

        // onDowngrade: present, a block, and empty
        val down = bodyOf(code, "onDowngrade")
        if (down == null) off += "no onDowngrade: SQLiteOpenHelper's default throws, and a reverted build cannot open its queue"
        else if (!down.block) off += "onDowngrade is not an empty block body"
        else if (down.text(code).isNotBlank())
            off += "onDowngrade does something (${down.text(code).trim().take(60)}); it must leave the database as it is"

        // onUpgrade: every statement an additive ADD COLUMN, in a runCatching, under a reachable version
        val adds = ArrayList<Add>()
        val up = bodyOf(code, "onUpgrade")
        if (up == null || !up.block) off += "no onUpgrade block"
        else {
            Regex("""\bdb\s*\.\s*(\w+)""").findAll(up.text(code)).map { it.groupValues[1] }
                .filter { it != "execSQL" }.forEach { off += "onUpgrade calls db.$it; an upgrade only ever ADDs a COLUMN" }
            for (st in statements(code, up)) {
                val where = "onUpgrade statement '${st.sql.trim().take(70)}'"
                if (st.notLiteral) { off += "$where is not one string literal, so it cannot be checked"; continue }
                val inner = st.openers.lastOrNull()
                if (inner == null || !Regex("""\brunCatching$""").containsMatchIn(inner.text))
                    off += "$where is outside a runCatching: a down-then-up cycle re-runs it over a column that exists"
                else if (code.substring(close(code, inner.at) + 1).trimStart().startsWith(".getOrThrow"))
                    off += "$where sits in a runCatching that rethrows"
                val since = st.openers.reversed().firstNotNullOfOrNull {
                    Regex("""\bif\s*\(\s*\w+\s*<\s*(\d+)\s*\)$""").find(it.text)?.groupValues?.get(1)?.toInt() }
                if (since == null) off += "$where is not under an `if (old < N)`"
                else if (version != null && (since < 2 || since > version))
                    off += "$where runs under `if (old < $since)`, past the helper's version $version, so it never runs"
                for (sql in expand(st, off, where)) {
                    val m = Regex("""^\s*ALTER\s+TABLE\s+captures\s+ADD\s+COLUMN\s+(\w+)\s+(\S.*?)\s*;?\s*$""",
                        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(sql)
                    if (m == null) { off += "$where is not an ADD COLUMN on captures; a schema change here is additive only"; continue }
                    val (col, raw) = m.destructured
                    val def = norm(raw)
                    off += columnRules(col, def)
                    adds += Add(since ?: 0, col, def)
                }
            }
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
        return Reading(version, adds, off)
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
        val sts = statements(code, b)
        val create = sts.firstNotNullOfOrNull {
            Regex("""^\s*CREATE\s+TABLE\s+captures\s*\((.*)\)\s*$""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(it.sql)?.groupValues?.get(1) }
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
    private class Stmt(val sql: String, val notLiteral: Boolean, val openers: List<Opener>)

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

    /** The `}` closing the `{` at [open]. */
    private fun close(code: String, open: Int): Int {
        var depth = 0; var i = open
        while (i < code.length) {
            val j = skipLiteral(code, i)
            if (j != i) { i = j; continue }
            when (code[i]) { '{' -> depth++; '}' -> { depth--; if (depth == 0) return i } }
            i++
        }
        error("unbalanced braces from offset $open")
    }

    /** The inside of `override fun [name](...) { ... }`: null when there is no such function. */
    private fun bodyOf(code: String, name: String): Body? {
        val m = Regex("""\boverride\s+fun\s+$name\s*\(""").find(code) ?: return null
        var depth = 0; var i = m.range.last
        while (i < code.length) {
            if (code[i] == '(') depth++ else if (code[i] == ')') { depth--; if (depth == 0) break }
            i++
        }
        var k = i + 1
        while (k < code.length && code[k].isWhitespace()) k++
        if (k >= code.length || code[k] != '{') return Body(k, k, block = false)
        return Body(k + 1, close(code, k), block = true)
    }

    /** Every `execSQL(` in [body], with its literal and the blocks it sits in, outermost first. */
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
                var k = i + "execSQL(".length
                while (k < code.length && code[k].isWhitespace()) k++
                val end = skipLiteral(code, k)
                val lit = if (end == k || code[k] == '\'') null
                    else if (code.startsWith("\"\"\"", k)) code.substring(k + 3, end - 3) else code.substring(k + 1, end - 1)
                var e = end
                while (e < code.length && code[e].isWhitespace()) e++
                val ok = lit != null && e < code.length && code[e] == ')'
                out += Stmt(lit ?: code.substring(k, minOf(k + 70, code.length)), !ok, stack.toList())
                i = maxOf(end, i + 1); continue
            }
            i++
        }
        return out
    }

    /** The statement's SQL once each `$name` is filled from an enclosing `for (name in listOf("a", "b"))`. */
    private fun expand(st: Stmt, off: MutableList<String>, where: String): List<String> {
        val vars = Regex("""\$\{?([A-Za-z_]\w*)\}?""").findAll(st.sql).map { it.groupValues[1] }.distinct().toList()
        if (vars.isEmpty()) return listOf(st.sql)
        if (vars.size > 1) { off += "$where fills more than one template, which this check cannot expand"; return emptyList() }
        val v = vars.single()
        val items = st.openers.reversed().firstNotNullOfOrNull {
            Regex("""\bfor\s*\(\s*$v\s+in\s+listOf\(([^()]*)\)\s*\)$""").find(it.text)?.groupValues?.get(1) }
        if (items == null || !Regex("""^\s*"\w+"(\s*,\s*"\w+")*\s*,?\s*$""").matches(items)) {
            off += "$where fills \$$v from something other than a listOf of column names"; return emptyList()
        }
        return Regex(""""(\w+)"""").findAll(items).map { n ->
            st.sql.replace(Regex("""\$\{?$v\}?(?!\w)"""), n.groupValues[1]) }.toList()
    }

    private fun lineOf(s: String, at: Int) = s.substring(0, at).count { it == '\n' } + 1
}
