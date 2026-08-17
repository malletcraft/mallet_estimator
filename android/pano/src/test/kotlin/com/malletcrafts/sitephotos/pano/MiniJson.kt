package com.malletcrafts.sitephotos.pano

/**
 * Just enough JSON to read the projection goldens.
 *
 * A dependency would do this better, but the goldens are the one thing that
 * must be readable with nothing installed and nothing resolved — if reading
 * the contract can break, the contract stops being checked, and the whole
 * point is that the two implementations cannot drift unnoticed.
 */
internal object MiniJson {

    fun parse(text: String): Any? {
        val p = Parser(text)
        p.skipWhitespace()
        val v = p.value()
        p.skipWhitespace()
        require(p.atEnd()) { "trailing content at ${p.pos}" }
        return v
    }

    private class Parser(private val s: String) {
        var pos = 0

        fun atEnd() = pos >= s.length

        fun skipWhitespace() {
            while (pos < s.length && s[pos].isWhitespace()) pos++
        }

        fun value(): Any? {
            skipWhitespace()
            return when (val c = s[pos]) {
                '{' -> obj()
                '[' -> arr()
                '"' -> str()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                else -> if (c == '-' || c.isDigit()) number()
                        else throw IllegalArgumentException("unexpected '$c' at $pos")
            }
        }

        private fun literal(word: String, v: Any?): Any? {
            require(s.startsWith(word, pos)) { "bad literal at $pos" }
            pos += word.length
            return v
        }

        private fun obj(): Map<String, Any?> {
            val out = LinkedHashMap<String, Any?>()
            pos++                                     // '{'
            skipWhitespace()
            if (s[pos] == '}') { pos++; return out }
            while (true) {
                skipWhitespace()
                val k = str()
                skipWhitespace()
                require(s[pos] == ':') { "expected ':' at $pos" }
                pos++
                out[k] = value()
                skipWhitespace()
                when (s[pos]) {
                    ',' -> pos++
                    '}' -> { pos++; return out }
                    else -> throw IllegalArgumentException("expected ',' or '}' at $pos")
                }
            }
        }

        private fun arr(): List<Any?> {
            val out = ArrayList<Any?>()
            pos++                                     // '['
            skipWhitespace()
            if (s[pos] == ']') { pos++; return out }
            while (true) {
                out.add(value())
                skipWhitespace()
                when (s[pos]) {
                    ',' -> pos++
                    ']' -> { pos++; return out }
                    else -> throw IllegalArgumentException("expected ',' or ']' at $pos")
                }
            }
        }

        private fun str(): String {
            require(s[pos] == '"') { "expected string at $pos" }
            pos++
            val sb = StringBuilder()
            while (s[pos] != '"') {
                if (s[pos] == '\\') {
                    pos++
                    when (val e = s[pos]) {
                        '"', '\\', '/' -> sb.append(e)
                        'b' -> sb.append('\b')
                        'f' -> sb.append('')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            sb.append(s.substring(pos + 1, pos + 5).toInt(16).toChar())
                            pos += 4
                        }
                        else -> throw IllegalArgumentException("bad escape at $pos")
                    }
                } else {
                    sb.append(s[pos])
                }
                pos++
            }
            pos++
            return sb.toString()
        }

        private fun number(): Number {
            val start = pos
            if (s[pos] == '-') pos++
            while (pos < s.length && (s[pos].isDigit() || s[pos] in ".eE+-")) pos++
            val raw = s.substring(start, pos)
            return if (raw.any { it in ".eE" }) raw.toDouble() else raw.toLong()
        }
    }
}
