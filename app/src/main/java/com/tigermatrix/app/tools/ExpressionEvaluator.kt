package com.tigermatrix.app.tools

import java.util.Locale
import kotlin.math.abs

/** حاسبة: + - * / وأقواس وأرقام عشرية. تقبل الأرقام العربية والرموز × ÷. */
object ExpressionEvaluator {

    fun normalizeDigits(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            when (c) {
                in '٠'..'٩' -> sb.append('0' + (c - '٠'))
                '٫' -> sb.append('.')
                '×' -> sb.append('*')
                '÷' -> sb.append('/')
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private val spokenWords = mapOf(
        "زائد" to "+", "زايد" to "+", "جمع" to "+",
        "ناقص" to "-", "طرح" to "-",
        "ضرب" to "*", "في" to "*",
        "قسمة" to "/", "تقسيم" to "/", "على" to "/"
    )

    /** يحوّل كلمات منطوقة مثل "5 ضرب 4" إلى "5 * 4" (للأوامر الصوتية). */
    fun normalizeSpoken(s: String): String =
        s.trim().split(Regex("\\s+")).joinToString(" ") { spokenWords[it] ?: it }

    fun evaluate(input: String): Double? = try {
        val s = normalizeDigits(normalizeSpoken(input)).replace(" ", "")
        if (s.isEmpty()) {
            null
        } else {
            val parser = Parser(s)
            val v = parser.parseExpr()
            if (parser.pos != s.length || v.isNaN() || v.isInfinite()) null else v
        }
    } catch (e: Exception) {
        null
    }

    fun evaluateToString(input: String): String? = evaluate(input)?.let { format(it) }

    fun format(d: Double): String {
        if (d == Math.rint(d) && abs(d) < 1e15) return d.toLong().toString()
        return String.format(Locale.US, "%.8f", d).trimEnd('0').trimEnd('.')
    }

    private class Parser(private val s: String) {
        var pos = 0

        private fun peek(): Char = if (pos < s.length) s[pos] else '\u0000'

        fun parseExpr(): Double {
            var v = parseTerm()
            while (true) {
                val c = peek()
                if (c == '+') { pos++; v += parseTerm() }
                else if (c == '-') { pos++; v -= parseTerm() }
                else break
            }
            return v
        }

        private fun parseTerm(): Double {
            var v = parseFactor()
            while (true) {
                val c = peek()
                if (c == '*') { pos++; v *= parseFactor() }
                else if (c == '/') {
                    pos++
                    val d = parseFactor()
                    if (d == 0.0) throw ArithmeticException("division by zero")
                    v /= d
                } else break
            }
            return v
        }

        private fun parseFactor(): Double {
            val c = peek()
            if (c == '-') { pos++; return -parseFactor() }
            if (c == '+') { pos++; return parseFactor() }
            if (c == '(') {
                pos++
                val v = parseExpr()
                if (peek() != ')') throw IllegalArgumentException("missing )")
                pos++
                return v
            }
            val start = pos
            while (peek().isDigit() || peek() == '.') pos++
            if (start == pos) throw IllegalArgumentException("number expected")
            return s.substring(start, pos).toDouble()
        }
    }
}
