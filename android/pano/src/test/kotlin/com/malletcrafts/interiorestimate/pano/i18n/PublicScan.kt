package com.malletcrafts.interiorestimate.pano.i18n

import com.malletcrafts.interiorestimate.pano.MiniJson
import java.io.File

/**
 * WHAT MAY NEVER ENTER THE PUBLIC REPOSITORY, as I18nTableTest checks 7 and 8 read it: every string a
 * table file holds -- values, notes, locations AND keys -- and where each hit is reported. Kept apart
 * from the test so the rules themselves are pinned on synthetic tables (PublicScanTest).
 */
internal object PublicScan {

    /** One string of a table file. [entry] is the top-level key's index; [isKey] when the string IS a key. */
    class Leaf(val file: String, val entry: Int, val path: String, val text: String, val isKey: Boolean)

    /**
     * Every string in [text], keys included: a key is built from the English and never renamed, so a
     * name or a number left in one would outlive the reworded text and reach index.tsv and Tx.kt.
     */
    @Suppress("UNCHECKED_CAST")
    fun leaves(file: String, text: String): List<Leaf> {
        val out = ArrayList<Leaf>()
        fun walk(entry: Int, path: String, v: Any?) {
            when (v) {
                is String -> out += Leaf(file, entry, path, v, false)
                is Map<*, *> -> v.forEach { (k, x) -> out += Leaf(file, entry, "$path{a key}", k.toString(), true); walk(entry, "$path.$k", x) }
                is List<*> -> v.forEachIndexed { i, x -> walk(entry, "$path[$i]", x) }
                else -> {}
            }
        }
        (MiniJson.parse(text) as Map<String, Any?>).entries.forEachIndexed { i, (k, v) ->
            out += Leaf(file, i, "$file:$k", k, true)
            walk(i, "$file:$k", v)
        }
        return out
    }

    /**
     * One offender per (leaf, rule) hit, NEVER with the matched text: these logs are public. A hit names
     * its path -- unless some KEY of that entry is itself a hit, when every hit of the entry names only
     * the entry's position, because the path would print the key, and the key is what matched.
     */
    fun offenders(leaves: List<Leaf>, rules: List<Pair<String, Regex>>): List<String> {
        val hits = leaves.flatMap { l -> rules.filter { it.second.containsMatchIn(l.text) }.map { l to it.first } }
        val withheld = hits.filter { it.first.isKey }.map { it.first.file to it.first.entry }.toSet()
        return hits.map { (l, rule) ->
            val where = if ((l.file to l.entry) in withheld) "${l.file}:entry #${l.entry} (${if (l.isKey) "a key" else "a value"}; the key is withheld)" else l.path
            "$where: matches $rule"
        }.distinct()
    }

    /** deny-patterns.txt: `name<TAB>Java regex`, comments `#`. */
    fun denyPatterns(f: File): Map<String, Regex> =
        f.readLines().filter { it.isNotBlank() && !it.startsWith("#") }.associate { line ->
            val i = line.indexOf('\t')
            require(i > 0) { "deny-patterns.txt: not 'name<TAB>regex': $line" }
            line.substring(0, i) to Regex(line.substring(i + 1))
        }

    /** A client name as a pattern: whole Latin words, any case; a name in another script as written. */
    fun nameRegex(n: String): Regex =
        if (n.any { it in 'A'..'Z' || it in 'a'..'z' }) Regex("(?i)(?<![A-Za-z])" + Regex.escape(n) + "(?![A-Za-z])") else Regex(Regex.escape(n))

    /**
     * The client-name list check 8 runs with. [names] null: none was given, and only the structural
     * patterns run -- [annotation] then says so as a `::warning`, so a CI log shows that names were NOT
     * checked; with a list it is a `::notice` with the count. [problems]: a list was ASKED for and cannot
     * be used (a path that is not a readable file, a list with no usable name); check 8 fails on them
     * rather than quietly running without names, and its own `::error` says so.
     */
    class DenyList(val names: List<String>?, val problems: List<String>, val annotation: String?)

    /** From the -Pi18n.deny file ([prop], the path as Gradle resolved it) or the I18N_DENY_NAMES secret ([env]). A path given is never passed over for the secret. */
    fun denyList(prop: String?, env: String?): DenyList {
        val (raw, source) = when {
            prop != null -> {
                val f = File(prop)
                if (!f.isFile || !f.canRead()) return DenyList(null,
                    listOf("-Pi18n.deny names $prop, which is not a readable file (a relative path is resolved against android/pano): the client-name list was asked for and could not be read"),
                    null)
                f.readText() to "-Pi18n.deny"
            }
            !env.isNullOrBlank() -> env to "I18N_DENY_NAMES"
            else -> return DenyList(null, emptyList(),
                "::warning title=i18n · 8 · names::client-name list absent (I18N_DENY_NAMES not set, no -Pi18n.deny): names were NOT checked, only the structural patterns")
        }
        val names = raw.split('\n', ',', ';').map { it.trim() }.filter { it.length >= 3 && !it.startsWith("#") }
        if (names.isEmpty()) return DenyList(names,
            listOf("the client-name list from $source holds no usable name (one per line or comma-separated, 3 characters or more)"),
            null)
        return DenyList(names, emptyList(), "::notice title=i18n · 8 · names::${names.size} client names checked ($source)")
    }
}
