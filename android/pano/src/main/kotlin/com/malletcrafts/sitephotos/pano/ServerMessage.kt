package com.malletcrafts.sitephotos.pano

/**
 * Turning a Frappe error reply into a sentence a person on a site can read.
 *
 * Amit, 2026-08-24, photographing the delete dialog on the phone. What it
 * said, in red, wrapped across seven lines and cut off mid-word:
 *
 *   frappe.exceptions.LinkExistsError: Cannot delete or cancel because Site
 *   Photo 360 <a href="https://mcft-stg.frappe.cloud/desk/site-photo-360/
 *   MEST-PH-2026-00027">MEST-PH-2026-00027</a> is linked with S
 *
 * Every part of that is wrong for the audience. The exception CLASS is a
 * Python detail. The anchor tag is markup nobody asked to see, and it repeats
 * the docname twice while burying it in a URL. And the truncation fell exactly
 * where the useful half begins — the sentence names the thing standing in the
 * way, and the phone cut it off before it could.
 *
 * Frappe already writes the sentence it means for humans into
 * `_server_messages`, so that is read first. `exception` is the fallback, and
 * it is stripped of its class prefix and its markup.
 *
 * Pure string work, kept in this module because this is the module CI runs
 * tests for — an error path is exactly the code that never gets exercised by
 * hand until the day it matters.
 */
object ServerMessage {

    /** Long enough for Frappe's link-exists sentence, which is the longest one
     *  that actually tells a person something. Past this a phone dialog is
     *  unreadable anyway. */
    const val MAX = 400

    /**
     * @param body the raw response body
     * @param code the HTTP status, used only when the body says nothing
     */
    fun humanise(body: String, code: Int): String {
        val fromServer = serverMessages(body).firstOrNull { it.isNotBlank() }
        val text = fromServer ?: exceptionSentence(body)
        val clean = clip(collapse(stripHtml(text)))
        return clean.ifBlank { "HTTP $code" }
    }

    /**
     * The `_server_messages` field is a JSON ARRAY OF JSON STRINGS — each entry
     * is itself a serialised object carrying `message`. Parsed by hand rather
     * than with a JSON library so this module stays dependency-free and
     * testable on a plain JVM.
     */
    fun serverMessages(body: String): List<String> {
        val raw = jsonStringField(body, "_server_messages") ?: return emptyList()
        // raw is now the decoded array text: ["{\"message\": \"...\"}", ...]
        val out = mutableListOf<String>()
        var i = 0
        while (i < raw.length) {
            val start = raw.indexOf('"', i)
            if (start < 0) break
            val (entry, after) = readJsonString(raw, start) ?: break
            i = after
            // Each entry is an object; its `message` is what a human should see.
            val msg = jsonStringField(entry, "message")
            if (msg != null) out.add(msg) else if (entry.isNotBlank()) out.add(entry)
        }
        return out
    }

    /** `"frappe.exceptions.LinkExistsError: Cannot delete…"` -> the half after
     *  the colon. A dotted class name is the marker; a colon inside an ordinary
     *  sentence must not trigger it. */
    fun exceptionSentence(body: String): String {
        val exc = jsonStringField(body, "exception") ?: return ""
        val colon = exc.indexOf(": ")
        if (colon <= 0) return exc
        val head = exc.substring(0, colon)
        val looksLikeAClass = head.contains('.') && !head.contains(' ')
        return if (looksLikeAClass) exc.substring(colon + 2) else exc
    }

    /** Tags out, entities back to characters. An anchor's TEXT is kept — that
     *  is where Frappe puts the docname — and its href discarded. */
    fun stripHtml(s: String): String {
        val sb = StringBuilder(s.length)
        var inTag = false
        for (c in s) {
            when {
                c == '<' -> inTag = true
                c == '>' -> inTag = false
                !inTag -> sb.append(c)
            }
        }
        return sb.toString()
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ")
    }

    fun collapse(s: String): String =
        s.replace(Regex("\\s+"), " ").trim()

    /** Truncates on a word boundary. Cutting "linked with Site Photo Inbox"
     *  after the S is how the original was useless. */
    fun clip(s: String): String {
        if (s.length <= MAX) return s
        val cut = s.lastIndexOf(' ', MAX - 1)
        return s.substring(0, if (cut > MAX / 2) cut else MAX - 1).trimEnd() + "…"
    }

    // --- the smallest JSON reader that does this job ----------------------

    /** The decoded value of a top-level `"name": "..."` pair, or null. */
    private fun jsonStringField(body: String, name: String): String? {
        val key = "\"$name\""
        var at = body.indexOf(key)
        while (at >= 0) {
            var i = at + key.length
            while (i < body.length && body[i].isWhitespace()) i++
            if (i < body.length && body[i] == ':') {
                i++
                while (i < body.length && body[i].isWhitespace()) i++
                if (i < body.length && body[i] == '"') {
                    return readJsonString(body, i)?.first
                }
            }
            at = body.indexOf(key, at + 1)
        }
        return null
    }

    /** Reads the string literal starting at [start] (which must be a quote).
     *  Returns the decoded contents and the index just past the closing quote. */
    private fun readJsonString(s: String, start: Int): Pair<String, Int>? {
        if (start >= s.length || s[start] != '"') return null
        val sb = StringBuilder()
        var i = start + 1
        while (i < s.length) {
            val c = s[i]
            when {
                c == '\\' && i + 1 < s.length -> {
                    val n = s[i + 1]
                    when (n) {
                        'n' -> sb.append('\n')
                        't' -> sb.append('\t')
                        'r' -> sb.append('\r')
                        'u' -> {
                            if (i + 5 < s.length) {
                                val hex = s.substring(i + 2, i + 6)
                                hex.toIntOrNull(16)?.let { sb.append(it.toChar()) }
                                i += 4
                            }
                        }
                        else -> sb.append(n)
                    }
                    i += 2
                }
                c == '"' -> return sb.toString() to (i + 1)
                else -> { sb.append(c); i++ }
            }
        }
        return null
    }
}
