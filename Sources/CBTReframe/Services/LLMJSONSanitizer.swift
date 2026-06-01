import Foundation

/// Gemini 等模型偶发输出残缺 JSON、混用中英文键名或夹杂说明文字；在解析前尽量裁出可解析片段。
enum LLMJSONSanitizer {
    static func sanitizeForJSONObject(_ raw: String) -> String {
        var text = raw
            .replacingOccurrences(of: "```json", with: "")
            .replacingOccurrences(of: "```JSON", with: "")
            .replacingOccurrences(of: "```", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)

        // 取第一个 { 与最后一个 } 之间的内容（忽略尾部截断时可能的多余字符）
        if let start = text.firstIndex(of: "{"),
           let end = text.lastIndex(of: "}") {
            text = String(text[start...end])
        }

        // 常见：未闭合字符串 — 尝试补全引号与括号（保守）
        text = repairTruncatedJSON(text)

        return text
    }

    /// 若末尾在字符串内被截断，补一个 `"` 并尝试闭合 `}`。
    private static func repairTruncatedJSON(_ s: String) -> String {
        var t = s
        let open = t.filter { $0 == "{" }.count
        let close = t.filter { $0 == "}" }.count
        if open > close {
            // 简单补全：若最后一个非空白字符不是 }，先尝试闭合字符串再补 }
            let trimmed = t.trimmingCharacters(in: .whitespacesAndNewlines)
            if let last = trimmed.last, last != "}" {
                if last != "\"" {
                    t += "\""
                }
                t += String(repeating: "}", count: open - close)
            }
        }
        return t
    }
}
