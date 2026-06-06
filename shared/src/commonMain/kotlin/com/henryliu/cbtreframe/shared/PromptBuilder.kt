package com.henryliu.cbtreframe.shared

import com.henryliu.cbtreframe.shared.CognitiveDistortion
import com.henryliu.cbtreframe.shared.ResponseStrategy
import com.henryliu.cbtreframe.shared.ThinkingTemplate
import com.henryliu.cbtreframe.shared.ThoughtEntry
import com.henryliu.cbtreframe.shared.detectRiskLevel
import com.henryliu.cbtreframe.shared.RiskLevel

object PromptBuilder {
    const val akathisiaMoodTag = "Akathisia"

    private fun isAkathisiaMood(mood: String): Boolean {
        return mood.trim() == akathisiaMoodTag
    }

    private fun shouldAttachAkathisiaInstructions(mood: String, hasAkathisia: Boolean): Boolean {
        return hasAkathisia || isAkathisiaMood(mood)
    }

    private val akathisiaSafetyInstructions = """
【Akathisia 补充】
用户标记了静坐不能（Akathisia）。请遵守：
- 承认其不安可能部分由生理因素驱动，不必把一切不适都归为「认知」或「性格」。
- 避免过度认知解释与长篇扭曲分析；不要替用户下绝对化结论。
- 通篇多用「可能」「似乎」「不一定」等不确定表述，少用断定句式。
""".trim()

    fun buildSystemPrompt(
        depth: ThinkingTemplate.AnalysisDepth,
        style: ThinkingTemplate.AppResponseStyle,
        template: ThinkingTemplate,
        mood: String = "",
        hasAkathisia: Boolean = false
    ): String {
        val roleIntro = "你是一位专业的认知行为治疗（CBT）辅助工具。你温和、有同理心、专业。"

        val templateInstructions: String = when (template) {
            ThinkingTemplate.cbt -> """
请对用户的想法进行认知行为治疗式的分析：
1. 识别其中可能存在的认知扭曲类型
2. 提供一个更平衡、更理性的替代想法
3. 建议一个具体的小行动来帮助用户
""".trim()
            ThinkingTemplate.socratic -> """
请使用苏格拉底提问法来引导用户反思：
1. 指出这个想法中可能存在的认知偏差
2. 提出2-3个引导性问题，帮助用户自己发现更平衡的视角
3. 建议一个反思练习
""".trim()
            ThinkingTemplate.behavioral -> """
请聚焦于行为激活，帮助用户从想法转向行动：
1. 简要分析这个想法的认知模式
2. 提供一个积极的替代视角
3. 给出一个具体的、可立即执行的行动步骤
""".trim()
        }

        val modeInstructions: String = when (depth) {
            ThinkingTemplate.AnalysisDepth.fast -> "请简洁回复，每部分1-2句话。"
            ThinkingTemplate.AnalysisDepth.balanced -> "请给出适中长度的回复，每部分2-3句话。"
            ThinkingTemplate.AnalysisDepth.deep -> "请给出详细深入的分析，每部分3-5句话，可以包含更多解释和例子。"
        }

        val styleInstructions: String = when (style) {
            ThinkingTemplate.AppResponseStyle.concise -> "风格要求：直接、简洁、专业。"
            ThinkingTemplate.AppResponseStyle.coach -> "风格要求：像一位温和但坚定的教练，鼓励用户成长。使用「你可以试试...」「想一想...」等引导性语言。"
            ThinkingTemplate.AppResponseStyle.supportive -> "风格要求：温暖、充满同理心。先认可用户的感受，再温柔地提供新视角。使用「我理解...」「这很正常...」等共情语言。"
        }

        val outputFormat = outputFormatJSON(forTemplate = template)

        val akathisiaBlock = if (shouldAttachAkathisiaInstructions(mood = mood, hasAkathisia = hasAkathisia)) {
            "\n\n$akathisiaSafetyInstructions\n"
        } else {
            ""
        }

        val result = """
$roleIntro

$templateInstructions

$modeInstructions
$styleInstructions
$akathisiaBlock
$outputFormat
""".trim()

        if (akathisiaBlock.isEmpty()) {
            return result.replace("$styleInstructions\n\n\n$outputFormat", "$styleInstructions\n\n$outputFormat")
        }
        return result
    }

    private fun outputFormatJSON(forTemplate: ThinkingTemplate): String {
        val distortionList = CognitiveDistortion.entries.joinToString("、") { it.displayName }
        return when (forTemplate) {
            ThinkingTemplate.cbt -> """
【输出要求】请严格按照以下 JSON 格式输出。
不要输出任何解释、前言、markdown标记或其他文字，只输出一个纯 JSON 对象：
{"distortion":"自动想法中的认知扭曲类型或简要描述","alternative":"更平衡的替代想法","action":"一条可执行的小行动","actions":["可选的更多行动建议"]}
distortion 必须优先从以下标准分类中选择：$distortionList；禁止填写「有返回结果」「成功」「OK」等系统状态或元信息。alternative 与 action 均须填写有效中文内容，不可留空。键名必须是英文 distortion, alternative, action。actions 可选。不要用 ```json 包裹。
""".trim()
            ThinkingTemplate.socratic -> """
【输出要求】请只输出一个纯 JSON 对象，不要直接给「答案」或替用户下结论。
格式：
{"distortion":"与想法相关的简短视角提示（非评判）","questions":["分步引导问题1","问题2","问题3"],"alternative":"一句总结：为何用提问而非直接答案","action":"反思练习或记录方式"}
键名必须是英文。questions 为必填：至少 2 条、每条至少 3 个字符，须为完整问句或引导句。不要用 ```json 包裹。
""".trim()
            ThinkingTemplate.behavioral -> """
【输出要求】强调行为与下一步，弱化长篇认知分析。只输出一个纯 JSON 对象：
{"stateAssessment":"当前状态与精力/情绪的简短评估","distortion":"一句话点出想法对行动的影响（若有）","alternative":"转向行为的一句鼓励","action":"唯一、可立即执行的下一步小行动（仅一步）"}
可选键 "actions" 不要用于长篇列举；优先填满 action 字段。不要用 ```json 包裹。
""".trim()
        }
    }

    fun buildSystemPrompt(
        depth: ThinkingTemplate.AnalysisDepth,
        style: ThinkingTemplate.AppResponseStyle,
        template: ThinkingTemplate,
        strategy: ResponseStrategy,
        mood: String = "",
        hasAkathisia: Boolean = false
    ): String {
        return when (strategy) {
            ResponseStrategy.cbtNormal -> buildSystemPrompt(depth = depth, style = style, template = template, mood = mood, hasAkathisia = hasAkathisia)
            ResponseStrategy.cbtGentle -> buildSystemPrompt(depth = depth, style = style, template = template, mood = mood, hasAkathisia = hasAkathisia) + "\n\n" + gentleAddon()
            ResponseStrategy.crisis -> crisisSystemPrompt()
        }
    }

    fun gentleAddon(): String {
        return """
【额外要求】
请减少分析的「评判感」，优先表达理解和陪伴。
不要过度纠正用户的想法，而是温和引导。
""".trim()
    }

    fun crisisSystemPrompt(): String {
        return """
你是一位支持性倾听者。

【重要】
- 不要分析认知扭曲
- 不要讲道理
- 不要给复杂建议

【你需要做】
1. 承认用户的痛苦
2. 表达理解和陪伴
3. 温和鼓励寻求现实支持（朋友/家人/专业帮助）

【输出要求】
只输出一段自然语言，不要 JSON，不要结构化字段。
""".trim()
    }

    fun reasonerAdditionalInstructions(): String {
        return """
【推理模型专用】你的思考过程用户看不到，也不要写出来。
对用户可见的输出只能是：上面要求的那一个 JSON 对象，且不要有任何其它字符（不要前言、不要 markdown、不要复述推理）。
字段必须简短：distortion 只写扭曲类型名称，不超过 12 个字；alternative、action 各不超过 3 句短句，每句尽量不超过 40 字。
不要把分析过程、举例或长段解释写进任一 JSON 字段。
""".trim()
    }

    fun buildUserPrompt(thought: String): String {
        return buildUserPrompt(thought = thought, mood = "", hasAkathisia = false)
    }

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
""".trim()
        }
        return """
用户选择的心情：$m
我的想法是：$thought
""".trim()
    }

    fun buildExternalPasteboardText(
        thought: String,
        mood: String,
        depth: ThinkingTemplate.AnalysisDepth,
        style: ThinkingTemplate.AppResponseStyle,
        template: ThinkingTemplate,
        strategy: ResponseStrategy,
        hasAkathisia: Boolean = false
    ): String {
        val system = buildSystemPrompt(
            depth = depth,
            style = style,
            template = template,
            strategy = strategy,
            mood = mood,
            hasAkathisia = hasAkathisia
        )
        val user = buildUserPrompt(thought = thought, mood = mood, hasAkathisia = hasAkathisia)
        return """
—— CBT Reframe · 外站使用 ——
在网页/App 中新建对话后：可将「系统提示」设为自定义说明，再发送「用户消息」；或整段一次性粘贴（视平台支持而定）。

【系统提示】
$system

【用户消息】
$user

—— 结束 ——
""".trim()
    }

    val thoughtPatternSystemPrompt = """
你是一位专业的认知行为治疗（CBT）辅助工具。请分析多条自动想法中的共性模式。
只输出一个纯 JSON 对象，不要输出任何解释、前言、markdown 标记或其他文字。
格式如下：
{"topDistortions":[{"name":"扭曲类型名","count":1,"example":"最典型的一条原文"}],"overallPattern":"整体思维模式总结","suggestion":"改善建议"}
注意：
1. 键名必须是英文 topDistortions, name, count, example, overallPattern, suggestion
2. topDistortions 最多返回 3 项
3. 所有值用中文
""".trim()

    fun buildThoughtPatternUserPrompt(thoughts: List<ThoughtEntry>): String {
        val thoughtsList = thoughts.mapIndexed { idx, entry ->
            var line = "${idx + 1}. \"${entry.content}\""
            if (entry.emotion.isNotEmpty()) { line += "（情绪: ${entry.emotion}）" }
            if (entry.situation.isNotEmpty()) { line += "（情境: ${entry.situation}）" }
            line
        }.joinToString("\n")

        return """
请分析以下自动想法列表，找出其中最常见的认知扭曲模式，并总结整体倾向：

$thoughtsList
""".trim()
    }

    fun containsCrisisContent(text: String): Boolean {
        return detectRiskLevel(text) == RiskLevel.high
    }
}
