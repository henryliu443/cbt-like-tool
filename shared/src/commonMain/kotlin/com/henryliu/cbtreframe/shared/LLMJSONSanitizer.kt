package com.henryliu.cbtreframe.shared

object LLMJSONSanitizer {
    /**
     * Strip markdown fences, extract the outermost JSON object between the
     * first `{` and last `}`, then repair truncation by counting braces and
     * appending missing quotes / closing braces.
     */
    fun sanitizeForJSONObject(raw: String): String {
        var text = raw
            .replace("```json", "")
            .replace("```JSON", "")
            .replace("```", "")
            .trim()

        val startIdx = text.indexOf('{')
        val endIdx = text.lastIndexOf('}')
        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            text = text.substring(startIdx, endIdx + 1)
        }

        text = repairTruncatedJSON(text)
        return text
    }

    // ── Private helpers ────────────────────────────────────────────────

    /**
     * Walk the JSON string character-by-character, tracking whether we are
     * inside a string literal (respecting backslash escapes).  Count open
     * `{` / `}` to compute the required closing-brace deficit.
     *
     * If the last meaningful token is an incomplete string (e.g. `"key":`
     * or `"value`), append a closing quote before the missing braces.
     */
    private fun repairTruncatedJSON(s: String): String {
        var braceDepth = 0
        var inString = false
        var escape = false
        var lastNonWhitespace: Char? = null

        for (ch in s) {
            if (escape) {
                escape = false
                continue
            }
            when {
                ch == '\\' && inString -> escape = true
                ch == '"' -> inString = !inString
                ch == '{' && !inString -> braceDepth++
                ch == '}' && !inString -> braceDepth--
            }
            if (!ch.isWhitespace()) lastNonWhitespace = ch
        }

        if (braceDepth <= 0) return s

        var result = s

        // Append a closing quote if we ended inside an unclosed string.
        if (inString && lastNonWhitespace != '"') {
            result += "\""
        }

        // Close remaining open braces.
        result += "}".repeat(braceDepth)

        return result
    }
}
