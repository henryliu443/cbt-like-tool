package com.henryliu.cbtreframe.shared

import kotlinx.serialization.Serializable

@Serializable
enum class CognitiveDistortion(
    val displayName: String
) {
    catastrophizing("灾难化"),
    blackAndWhite("非黑即白"),
    overgeneralization("过度概括"),
    mindReading("读心术"),
    emotionalReasoning("情绪化推理"),
    shouldStatement("应该思维"),
    labeling("贴标签"),
    personalization("个人化"),
    fortuneTelling("预言未来"),
    mentalFilter("心理过滤");

    val description: String
        get() = when (this) {
            catastrophizing -> "认为最坏的结果必然发生，低估自己应对的能力。"
            blackAndWhite -> "以极端的两极看待事物，忽略中间地带。"
            overgeneralization -> "从单次事件中得出普遍性结论。"
            mindReading -> "假设自己知道他人的想法和意图，而不求证。"
            emotionalReasoning -> "将情绪当作事实，认为自己的感受就是真相。"
            shouldStatement -> "用「应该」「必须」等绝对化要求对待自己和他人。"
            labeling -> "给自己或他人贴上负面标签，而非描述具体行为。"
            personalization -> "将外部事件全部归因于自己，忽视其他因素。"
            fortuneTelling -> "预测未来一定会负面，而没有客观证据。"
            mentalFilter -> "只关注负面细节，过滤掉积极信息。"
        }

    val educationTip: String
        get() = when (this) {
            catastrophizing -> "把最坏结果当成必然。试着列出三种更现实的可能。"
            blackAndWhite -> "世界常常不是0或1，尝试找出中间地带。"
            overgeneralization -> "一次失败不等于一直失败，回看反例。"
            mindReading -> "你无法直接读心，先用事实验证。"
            emotionalReasoning -> "感觉很真实，但不一定等于事实。"
            shouldStatement -> "把「必须」改成「我希望」，降低自我苛责。"
            labeling -> "行为不等于身份，用具体描述替代标签。"
            personalization -> "很多结果受多因素影响，不必全归因于自己。"
            fortuneTelling -> "未来未发生，改成「可能」并准备备选方案。"
            mentalFilter -> "同时记录负面与正面证据，避免单一过滤。"
        }

    companion object {
        fun detect(text: String): CognitiveDistortion? {
            val lower = text.lowercase()
            val map = listOf(
                catastrophizing to listOf("灾难", "完蛋", "最糟", "catastroph"),
                blackAndWhite to listOf("非黑即白", "要么", "完全", "all or nothing"),
                overgeneralization to listOf("总是", "从来", "每次", "过度概括"),
                mindReading to listOf("别人一定", "他们觉得", "读心"),
                emotionalReasoning to listOf("我感觉", "所以事实", "情绪化推理"),
                shouldStatement to listOf("应该", "必须", "不能这样"),
                labeling to listOf("我是废物", "没用", "标签"),
                personalization to listOf("都怪我", "我的错", "个人化"),
                fortuneTelling to listOf("肯定会失败", "一定会", "预言"),
                mentalFilter to listOf("只看到", "忽略好的", "过滤"),
            )
            for ((kind, words) in map) {
                if (words.any { lower.contains(it.lowercase()) }) {
                    return kind
                }
            }
            return null
        }
    }
}
