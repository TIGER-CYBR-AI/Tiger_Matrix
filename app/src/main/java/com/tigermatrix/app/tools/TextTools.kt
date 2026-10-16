package com.tigermatrix.app.tools

import com.tigermatrix.app.assistant.ArabicText

object TextTools {
    data class Stats(val chars: Int, val words: Int, val lines: Int)

    fun stats(text: String): Stats = Stats(
        chars = text.length,
        words = text.trim().split(Regex("\\s+")).count { it.isNotEmpty() },
        lines = if (text.isEmpty()) 0 else text.lines().size
    )

    fun removeDiacritics(text: String): String = ArabicText.stripDiacritics(text)

    fun cleanSpaces(text: String): String =
        text.lines()
            .joinToString("\n") { it.trim().replace(Regex("[ \\t]+"), " ") }
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
}
