package com.henryliu.cbtreframe.shared

object PromptBuilder {
    const val akathisiaMoodTag = "Akathisia"

    private fun isAkathisiaMood(mood: String): Boolean =
        mood.trim() == akathisiaMoodTag

    private fun shouldAttachAkathisiaInstructions(mood: String, hasAkathisia: Boolean): Boolean =
        hasAkathisia || isAkathisiaMood(mood)

    private const val akathisiaSafetyInstructions = """
【Akathisia 补充】
用户标记了静坐不能（Akathisia）。请遵守：
- 承认其不安可能部分由生理因素驱动，不必把一切不适都归为「认知」或「性格」。
- 避免过度认知解释与长篇扭曲分析；不要替用户下绝对化结论。
- 通篇多用「可能」「似乎」「不一定」等不确定表述，少用断定句式。
"""

    fun buildSystemPrompt(
        template: ThinkingTemplate,
        strategy: ResponseStrategy,
        depth: ThinkingTemplate.AnalysisDepth,
        style: ThinkingTemplate.AppResponseStyle,
        mood: String = "",
        hasAkathisia: Boolean = false,
    ): String {
        if (strategy == ResponseStrategy.crisis) {
            return crisisSystemPrompt()
        }

        val roleIntro = "你是一位专业的认知行为治疗（CBT）辅助工具。你温和、有同理心、专业。"

        val templateInstructions = when (template) {
            ThinkingTemplate.cbt -> """
请对用户的想法进行认知行为治疗式的分析：
1. 识别其中可能存在的认知扭曲类型
2. 提供一个更平衡、更理性的替代想法
3. 建议一个具体的小行动来帮助用户
"""
            ThinkingTemplate.socratic -> """
请使用苏格拉底提问法来引导用户反思：
1. 指出这个想法中可能存在的认知偏差
2. 提出2-3个引导性问题，帮助用户自己发现更平衡的视角
3. 建议一个反思练习
"""
            ThinkingTemplate.behavioral -> """
请聚焦于行为激活，帮助用户从想法转向行动：
1. 简要分析这个想法的认知模式
2. 提供一个积极的替代视角
3. 给出一个具体的、可立即执行的行动步骤
"""
        }

        val depthInstructions = when (depth) {
            ThinkingTemplate.AnalysisDepth.fast -> "请简洁回复，每部分1-2句话。"
            ThinkingTemplate.AnalysisDepth.balanced -> "请给出适中长度的回复，每部分2-3句话。"
            ThinkingTemplate.AnalysisDepth.deep -> "请给出详细深入的分析，每部分3-5句话，可以包含更多解释和例子。"
        }

        val styleInstructions = when (style) {
            ThinkingTemplate.AppResponseStyle.concise -> "风格要求：直接、简洁、专业。"
            ThinkingTemplate.AppResponseStyle.coach -> "风格要求：像一位温和但坚定的教练，鼓励用户成长。使用「你可以试试...」「想一想...」等引导性语言。"
            ThinkingTemplate.AppResponseStyle.supportive -> "风格要求：温暖、充满同理心。先认可用户的感受，再温柔地提供新视角。使用「我理解...」「这很正常...」等共情语言。"
        }

        val outputFormat = outputFormatJSON(template)

        val akathisiaBlock = if (shouldAttachAkathisiaInstructions(mood, hasAkathisia)) {
            "\n\n$akathisiaSafetyInstructions\n"
        } else ""

        return """
$roleIntro

$templateInstructions

$depthInstructions
$styleInstructions
$akathisiaBlock
$outputFormat
""".trimIndent()
    }

    private fun outputFormatJSON(template: ThinkingTemplate): String {
        val distortionList = "灾难化、非黑即白、过度概括、读心术、情绪化推理、应该思维、贴标签、个人化、预言未来、心理过滤"
        return when (template) {
            ThinkingTemplate.cbt -> """
【输出要求】请严格按照以下 JSON 格式输出。
不要输出任何解释、前言、markdown标记或其他文字，只输出一个纯 JSON 对象：
{"distortion":"自动想法中的认知扭曲类型或简要描述","alternative":"更平衡的替代想法","action":"一条可执行的小行动","actions":["可选的更多行动建议"]}
distortion 必须优先从以下标准分类中选择：$distortionList；禁止填写「有返回结果」「成功」「OK」等系统状态或元信息。alternative 与 action 均须填写有效中文内容，不可留空。键名必须是英文 distortion, alternative, action。actions 可选。
"""
            ThinkingTemplate.socratic -> """
【输出要求】请严格按照以下 JSON 格式输出。
不要输出任何解释、前言、markdown标记或其他文字，只输出一个纯 JSON 对象：
{"distortion":"可能的认知偏差类型","alternative":"反思引导语","action":"反思练习","questions":["引导问题1","引导问题2","引导问题3"],"state_assessment":"可选状态评估"}
distortion 必须优先从以下标准分类中选择：$distortionList。alternative 写一句温和的引导语。questions 必须为字符串数组且至少包含2条问题。键名用英文。
"""
            ThinkingTemplate.behavioral -> """
【输出要求】请严格按照以下 JSON 格式输出。
不要输出任何解释、前言、markdown标记或其他文字，只输出一个纯 JSON 对象：
{"distortion":"当前行为模式","alternative":"积极的替代视角","action":"一个具体的、可立即执行的行动步骤","actions":["可选更多行动建议"],"state_assessment":"精力与情绪状态评估"}
distortion 必须优先从以下标准分类中选择：$distortionList。action 必须具体可执行。键名用英文。
"""
        }
    }

    private const val gentleAddon = """
【语气要求】
- 用户可能处于敏感状态
- 请用温和、鼓励的语气
- 先认可用户的感受，再提供新视角
- 避免任何批评性语言
"""

    private fun crisisSystemPrompt(): String = """
你是一位专业且有同理心的心理危机支持人员。

【核心原则】
- 用户正处于心理危机中，需要被倾听和理解
- 你的首要目标是提供情感支持和减轻痛苦
- 不要做深入分析或认知重构
- 不要给复杂建议

【你需要做】
1. 承认用户的痛苦
2. 表达理解和陪伴
3. 温和鼓励寻求现实支持（朋友/家人/专业帮助）

【输出要求】
只输出一段自然语言，不要 JSON，不要结构化字段。
""".trimIndent()

    fun reasonerAdditionalInstructions(): String = """
【推理模型专用】你的思考过程用户看不到，也不要写出来。
对用户可见的输出只能是：上面要求的那一个 JSON 对象，且不要有任何其它字符（不要前言、不要 markdown、不要复述推理）。
字段必须简短：distortion 只写扭曲类型名称，不超过 12 个字；alternative、action 各不超过 3 句短句，每句尽量不超过 40 字。
不要把分析过程、举例或长段解释写进任一 JSON 字段。
""".trimIndent()

    fun buildUserPrompt(thought: String): String =
        buildUserPrompt(thought = thought, mood = "")

    fun buildUserPrompt(thought: String, mood: String, hasAkathisia: Boolean = false): String {
        val m = mood.trim()
        if (m.isEmpty()) {
            return "我的想法是：$thought"
        }
        if (hasAkathisia && !isAkathisiaMood(m)) {
            return """
用户选择的心情：$m
用户另标记：存在静坐不能（Akathisia），身体不适可能与生理因素有关。
我的想法是：$thought
""".trimIndent()
        }
        return """
用户选择的心情：$m
我的想法是：$thought
""".trimIndent()
    }

    val thoughtPatternSystemPrompt: String = """
你是一位专业的认知行为治疗（CBT）辅助工具。请分析多条自动想法中的共性模式。
只输出一个纯 JSON 对象，不要输出任何解释、前言、markdown 标记或其他文字。
格式如下：
{"topDistortions":[{"name":"扭曲类型名","count":1,"example":"最典型的一条原文"}],"overallPattern":"整体思维模式总结","suggestion":"改善建议"}
注意：
1. 键名必须是英文 topDistortions, name, count, example, overallPattern, suggestion
2. topDistortions 最多返回 3 项
3. 所有值用中文
""".trimIndent()

    fun buildThoughtPatternUserPrompt(thoughts: List<ThoughtEntry>): String {
        val thoughtsList = thoughts.mapIndexed { idx, entry ->
            val parts = mutableListOf<String>()
            parts.add("${idx + 1}. \"${entry.content}\"")
            if (entry.emotion.isNotEmpty()) parts.add("（情绪: ${entry.emotion}）")
            if (entry.situation.isNotEmpty()) parts.add("（情境: ${entry.situation}）")
            parts.joinToString(" ")
        }.joinToString("\n")

        return """
请分析以下自动想法列表，找出其中最常见的认知扭曲模式，并总结整体倾向：

$thoughtsList
""".trimIndent()
    }
}
