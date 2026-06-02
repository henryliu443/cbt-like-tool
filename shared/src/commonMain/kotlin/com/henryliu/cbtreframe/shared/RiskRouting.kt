package com.henryliu.cbtreframe.shared

import kotlinx.serialization.Serializable

// ── Risk Level ──────────────────────────────────────────────────────────────

@Serializable
enum class RiskLevel { safe, low, medium, high }

// ── Response Strategy ───────────────────────────────────────────────────────

@Serializable
enum class ResponseStrategy { cbtNormal, cbtGentle, crisis }

// ── Keyword ─────────────────────────────────────────────────────────────────

@Serializable
data class RiskKeyword(
    val word: String,
    val weight: Int
)

// ── Lexicon ─────────────────────────────────────────────────────────────────

object RiskLexicon {
    /**
     * 命中任一则直接视为高风险并本地拦截，不依赖累计分数
     * （避免「单条高危词但总分不够」仍去打 API）。
     */
    val immediateBlockPhrases: List<String> = listOf(
        "自杀", "轻生", "结束生命", "了结", "割腕", "跳楼", "死了算了", "去死", "不想活",
        "kill myself", "suicide", "end my life", "want to die",
    )

    /**
     * 关键词命中即累加对应权重；同一词在文中多次出现仍只加一次（按词表项计）。
     */
    val keywords: List<RiskKeyword> = listOf(
        RiskKeyword("自杀", 10),
        RiskKeyword("结束生命", 10),
        RiskKeyword("kill myself", 10),
        RiskKeyword("suicide", 10),
        RiskKeyword("want to die", 9),
        RiskKeyword("end my life", 10),

        RiskKeyword("不想活", 6),
        RiskKeyword("活着没意思", 5),
        RiskKeyword("死了算了", 8),
        RiskKeyword("去死", 8),
        RiskKeyword("轻生", 10),
        RiskKeyword("了结", 10),
        RiskKeyword("跳楼", 8),
        RiskKeyword("割腕", 8),

        RiskKeyword("很累", 2),
        RiskKeyword("撑不住", 4),
    )
}

// ── Scoring ─────────────────────────────────────────────────────────────────

fun calculateRiskScore(text: String): Int {
    val lower = text.lowercase()
    var score = 0
    for (keyword in RiskLexicon.keywords) {
        if (lower.contains(keyword.word.lowercase())) {
            score += keyword.weight
        }
    }
    return score
}

fun hasImmediateCrisisKeyword(text: String): Boolean {
    val lower = text.lowercase()
    for (phrase in RiskLexicon.immediateBlockPhrases) {
        if (lower.contains(phrase.lowercase())) {
            return true
        }
    }
    return false
}

fun detectRiskLevel(text: String): RiskLevel {
    if (hasImmediateCrisisKeyword(text)) {
        return RiskLevel.high
    }
    val score = calculateRiskScore(text)
    return when {
        score < 3 -> RiskLevel.safe
        score < 6 -> RiskLevel.low
        score < 10 -> RiskLevel.medium
        else -> RiskLevel.high
    }
}

fun routeStrategy(level: RiskLevel): ResponseStrategy = when (level) {
    RiskLevel.safe, RiskLevel.low -> ResponseStrategy.cbtNormal
    RiskLevel.medium -> ResponseStrategy.cbtGentle
    RiskLevel.high -> ResponseStrategy.crisis
}

fun isJsonMode(strategy: ResponseStrategy): Boolean = strategy != ResponseStrategy.crisis

/**
 * 高风险内容：不调用任何远端 LLM（避免安全拒答、空回复与 API 费用），
 * 仅用本地固定支持文案。
 */
fun shouldUseLocalCrisisOnly(text: String): Boolean =
    routeStrategy(detectRiskLevel(text)) == ResponseStrategy.crisis

// ── Local Crisis Reply ──────────────────────────────────────────────────────
// （与远端无关，单一路径供 UI / 历史记录使用）

object CrisisLocalSupport {
    val analysisResult = AnalysisResult(
        distortion = "支持与陪伴",
        alternative = "听起来你正在承受很大的痛苦，你愿意说出来已经很不容易。你值得被认真对待，不必独自扛下所有。若你感到难以承受，请尽量联系你信任的人陪伴在身边；紧急情况请拨打当地急救或心理危机热线。",
        action = "若情绪持续或加重，请向信任的人求助，或联系当地心理援助热线与专业医疗机构。"
    )

    const val historyProviderName = "本地"
    const val historyModelName = "危机支持"
}
