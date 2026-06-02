package com.henryliu.cbtreframe.shared

/**
 * Gemini 等模型偶发输出残缺 JSON、混用中英文键名或夹杂说明文字；在解析前尽量裁出可解析片段。
 */
object LLMJSONSanitizer {
    fun sanitizeForJSONObject(raw: String): String {
        var text = raw
            .replace("```json", "", ignoreCase = true)
            .replace("```", "")
            .trim()

        // 取第一个 { 与最后一个 } 之间的内容（忽略尾部截断时可能的多余字符）
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start != -1 && end != -1 && start <= end) {
            text = text.substring(start, end + 1)
        }

        // 常见：未闭合字符串 — 尝试补全引号与括号（保守）
        text = repairTruncatedJSON(text)

        return text
    }

    /**
     * 若末尾在字符串内被截断，补一个 `"` 并尝试闭合 `}`。
     */
    private fun repairTruncatedJSON(s: String): String {
        var t = s
        val open = t.count { it == '{' }
        val close = t.count { it == '}' }
        if (open > close) {
            // 简单补全：若最后一个非空白字符不是 }，先尝试闭合字符串再补 }
            val trimmed = t.trim()
            if (trimmed.isNotEmpty() && trimmed.last() != '}') {
                if (trimmed.last() != '"') {
                    t += "\""
                }
                t += "}".repeat(open - close)
            }
        }
        return t
    }
}
