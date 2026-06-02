package com.henryliu.cbtreframe.shared

import kotlinx.serialization.json.*

fun parseReframeOutput(content: String, strategy: ResponseStrategy): AnalysisResult {
    if (strategy == ResponseStrategy.crisis) {
        return parsePlainTextCrisisResponse(content)
    }
    return parseJSONContent(content)
}

fun parsePlainTextCrisisResponse(content: String): AnalysisResult {
    var text = content
        .replace("```", "")
        .trim()
    if (text.isEmpty()) {
        text = "你愿意说出来，这本身就很不容易。你值得被认真对待，也有人愿意陪伴你度过这段艰难的时刻。"
    }
    return AnalysisResult(
        distortion = "支持与陪伴",
        alternative = text,
        action = "若情绪持续或加重，请向信任的人求助，或联系当地心理援助热线与专业医疗机构。"
    )
}

fun parseJSONContent(content: String): AnalysisResult {
    val text = LLMJSONSanitizer.sanitizeForJSONObject(content)

    // Try direct deserialization first
    try {
        return json.decodeFromString(AnalysisResult.serializer(), text)
    } catch (_: Exception) {
        // Fallback: parse manually
    }

    val jsonObj = parseJsonObject(text)
    if (jsonObj != null) {
        val distortion = jsonObj["distortion"]?.jsonPrimitive?.contentOrNull
            ?: jsonObj["认知扭曲"]?.jsonPrimitive?.contentOrNull
            ?: jsonObj["cognitive_distortion"]?.jsonPrimitive?.contentOrNull
            ?: "未识别"

        val alternative = jsonObj["alternative"]?.jsonPrimitive?.contentOrNull
            ?: jsonObj["替代想法"]?.jsonPrimitive?.contentOrNull
            ?: jsonObj["alternative_thought"]?.jsonPrimitive?.contentOrNull
            ?: ""

        val action = jsonObj["action"]?.jsonPrimitive?.contentOrNull
            ?: jsonObj["建议行动"]?.jsonPrimitive?.contentOrNull
            ?: jsonObj["小行动"]?.jsonPrimitive?.contentOrNull
            ?: jsonObj["suggested_action"]?.jsonPrimitive?.contentOrNull
            ?: jsonObj["nextStep"]?.jsonPrimitive?.contentOrNull
            ?: jsonObj["next_step"]?.jsonPrimitive?.contentOrNull
            ?: ""

        val questions = parseStringArray(jsonObj, listOf("questions", "引导问题", "socratic_questions", "question_list"))
        val actions = parseStringArray(jsonObj, listOf("actions", "行动建议", "action_steps"))
        val stateAssessment = jsonObj["stateAssessment"]?.jsonPrimitive?.contentOrNull
            ?: jsonObj["state_assessment"]?.jsonPrimitive?.contentOrNull
            ?: jsonObj["当前状态"]?.jsonPrimitive?.contentOrNull
            ?: jsonObj["状态评估"]?.jsonPrimitive?.contentOrNull

        return AnalysisResult(
            distortion = distortion,
            alternative = alternative,
            action = action,
            questions = questions,
            actions = actions,
            stateAssessment = stateAssessment,
        )
    }

    // Last resort: line-based fallback
    val lines = content.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    if (lines.size >= 3) {
        return AnalysisResult(
            distortion = lines[0],
            alternative = lines[1],
            action = lines[2],
        )
    }

    return AnalysisResult(
        distortion = "AI 分析",
        alternative = content,
        action = "请尝试重新分析",
    )
}

internal fun parseJsonObject(data: String): JsonObject? {
    return try {
        json.parseToJsonElement(data).jsonObject
    } catch (_: Exception) {
        null
    }
}

private fun parseStringArray(jsonObj: JsonObject, keys: List<String>): List<String>? {
    for (key in keys) {
        val element = jsonObj[key] ?: continue
        when {
            element.jsonArray != null -> {
                val cleaned = element.jsonArray.mapNotNull { it.jsonPrimitive?.contentOrNull?.trim() }
                    .filter { it.isNotEmpty() }
                if (cleaned.isNotEmpty()) return cleaned
            }
            element.jsonPrimitive?.contentOrNull != null -> {
                val parts = element.jsonPrimitive.contentOrNull!!
                    .split("\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                if (parts.isNotEmpty()) return parts
            }
        }
    }
    return null
}

fun parseThoughtPatternContent(content: String): ThoughtPatternReport {
    var text = content
        .replace("```json", "")
        .replace("```", "")
        .trim()

    val startIdx = text.indexOf('{')
    val endIdx = text.lastIndexOf('}')
    if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
        text = text.substring(startIdx, endIdx + 1)
    }

    return try {
        json.decodeFromString(ThoughtPatternReport.serializer(), text)
    } catch (e: Exception) {
        throw AIServiceError.ParseError("模式分析 JSON 解析失败")
    }
}
