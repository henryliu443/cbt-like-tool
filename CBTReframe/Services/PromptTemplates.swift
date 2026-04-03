import Foundation

struct PromptBuilder {
    static func buildSystemPrompt(mode: ReframeMode, style: ResponseStyle, template: PromptTemplate) -> String {
        let roleIntro = "你是一位专业的认知行为治疗（CBT）辅助工具。你温和、有同理心、专业。"

        let templateInstructions: String
        switch template {
        case .cbtReframe:
            templateInstructions = """
            请对用户的想法进行认知行为治疗式的分析：
            1. 识别其中可能存在的认知扭曲类型
            2. 提供一个更平衡、更理性的替代想法
            3. 建议一个具体的小行动来帮助用户
            """
        case .socratic:
            templateInstructions = """
            请使用苏格拉底提问法来引导用户反思：
            1. 指出这个想法中可能存在的认知偏差
            2. 提出2-3个引导性问题，帮助用户自己发现更平衡的视角
            3. 建议一个反思练习
            """
        case .behavioral:
            templateInstructions = """
            请聚焦于行为激活，帮助用户从想法转向行动：
            1. 简要分析这个想法的认知模式
            2. 提供一个积极的替代视角
            3. 给出一个具体的、可立即执行的行动步骤
            """
        }

        let modeInstructions: String
        switch mode {
        case .quick:
            modeInstructions = "请简洁回复，每部分1-2句话。"
        case .balanced:
            modeInstructions = "请给出适中长度的回复，每部分2-3句话。"
        case .deep:
            modeInstructions = "请给出详细深入的分析，每部分3-5句话，可以包含更多解释和例子。"
        }

        let styleInstructions: String
        switch style {
        case .brief:
            styleInstructions = "风格要求：直接、简洁、专业。"
        case .coach:
            styleInstructions = "风格要求：像一位温和但坚定的教练，鼓励用户成长。使用「你可以试试...」「想一想...」等引导性语言。"
        case .warm:
            styleInstructions = "风格要求：温暖、充满同理心。先认可用户的感受，再温柔地提供新视角。使用「我理解...」「这很正常...」等共情语言。"
        }

        let outputFormat = outputFormatJSON(for: template)

        return """
        \(roleIntro)

        \(templateInstructions)

        \(modeInstructions)
        \(styleInstructions)

        \(outputFormat)
        """
    }

    /// 各疗法使用不同 JSON 结构，便于解析与分开展示。
    private static func outputFormatJSON(for template: PromptTemplate) -> String {
        switch template {
        case .cbtReframe:
            return """
            【输出要求】请严格按照以下 JSON 格式输出。
            不要输出任何解释、前言、markdown标记或其他文字，只输出一个纯 JSON 对象：
            {"distortion":"自动想法中的认知扭曲类型或简要描述","alternative":"更平衡的替代想法","action":"一条可执行的小行动","actions":["可选的更多行动建议"]}
            注意：键名必须是英文 distortion, alternative, action。actions 可选。值用中文。不要用 ```json 包裹。
            """
        case .socratic:
            return """
            【输出要求】请只输出一个纯 JSON 对象，不要直接给「答案」或替用户下结论。
            格式：
            {"distortion":"与想法相关的简短视角提示（非评判）","questions":["分步引导问题1","问题2","问题3"],"alternative":"一句总结：为何用提问而非直接答案","action":"反思练习或记录方式"}
            键名必须是英文。questions 为必填：至少 2 条、每条至少 3 个字符，须为完整问句或引导句。不要用 ```json 包裹。
            """
        case .behavioral:
            return """
            【输出要求】强调行为与下一步，弱化长篇认知分析。只输出一个纯 JSON 对象：
            {"stateAssessment":"当前状态与精力/情绪的简短评估","distortion":"一句话点出想法对行动的影响（若有）","alternative":"转向行为的一句鼓励","action":"唯一、可立即执行的下一步小行动（仅一步）"}
            可选键 "actions" 不要用于长篇列举；优先填满 action 字段。不要用 ```json 包裹。
            """
        }
    }

    /// 根据风险路由选择系统提示：高风险走危机支持（纯文本），中风险在 CBT 上叠加温和约束。
    static func buildSystemPrompt(
        mode: ReframeMode,
        style: ResponseStyle,
        template: PromptTemplate,
        strategy: ResponseStrategy
    ) -> String {
        switch strategy {
        case .cbtNormal:
            return buildSystemPrompt(mode: mode, style: style, template: template)
        case .cbtGentle:
            return buildSystemPrompt(mode: mode, style: style, template: template) + "\n\n" + gentleAddon()
        case .crisis:
            return crisisSystemPrompt()
        }
    }

    static func gentleAddon() -> String {
        """
        【额外要求】
        请减少分析的「评判感」，优先表达理解和陪伴。
        不要过度纠正用户的想法，而是温和引导。
        """
    }

    static func crisisSystemPrompt() -> String {
        """
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
        """
    }

    /// 仅用于 DeepSeek Reasoner 等「先推理再作答」的模型：避免把推理过程写入 JSON 或拉长字段。
    static func reasonerAdditionalInstructions() -> String {
        """
        【推理模型专用】你的思考过程用户看不到，也不要写出来。
        对用户可见的输出只能是：上面要求的那一个 JSON 对象，且不要有任何其它字符（不要前言、不要 markdown、不要复述推理）。
        字段必须简短：distortion 只写扭曲类型名称，不超过 12 个字；alternative、action 各不超过 3 句短句，每句尽量不超过 40 字。
        不要把分析过程、举例或长段解释写进任一 JSON 字段。
        """
    }

    static func buildUserPrompt(thought: String) -> String {
        buildUserPrompt(thought: thought, mood: "")
    }

    /// `mood` 为用户必选的心情标签，便于模型结合情绪语境解读自动想法。
    static func buildUserPrompt(thought: String, mood: String) -> String {
        let m = mood.trimmingCharacters(in: .whitespacesAndNewlines)
        if m.isEmpty {
            return "我的想法是：\(thought)"
        }
        return """
        用户选择的心情：\(m)
        我的想法是：\(thought)
        """
    }

    /// 与 App 内请求一致，供用户复制到 DeepSeek / ChatGPT 网页或 App，无需消耗本应用 API。
    static func buildExternalPasteboardText(
        thought: String,
        mood: String,
        mode: ReframeMode,
        style: ResponseStyle,
        template: PromptTemplate,
        strategy: ResponseStrategy
    ) -> String {
        let system = buildSystemPrompt(mode: mode, style: style, template: template, strategy: strategy)
        let user = buildUserPrompt(thought: thought, mood: mood)
        return """
        —— CBT Reframe · 外站使用 ——
        在网页/App 中新建对话后：可将「系统提示」设为自定义说明，再发送「用户消息」；或整段一次性粘贴（视平台支持而定）。

        【系统提示】
        \(system)

        【用户消息】
        \(user)

        —— 结束 ——
        """
    }

    static let thoughtPatternSystemPrompt = """
    你是一位专业的认知行为治疗（CBT）辅助工具。请分析多条自动想法中的共性模式。
    只输出一个纯 JSON 对象，不要输出任何解释、前言、markdown 标记或其他文字。
    格式如下：
    {"topDistortions":[{"name":"扭曲类型名","count":1,"example":"最典型的一条原文"}],"overallPattern":"整体思维模式总结","suggestion":"改善建议"}
    注意：
    1. 键名必须是英文 topDistortions, name, count, example, overallPattern, suggestion
    2. topDistortions 最多返回 3 项
    3. 所有值用中文
    """

    static func buildThoughtPatternUserPrompt(thoughts: [ThoughtEntry]) -> String {
        let thoughtsList = thoughts.enumerated().map { idx, entry in
            var line = "\(idx + 1). \"\(entry.content)\""
            if !entry.emotion.isEmpty { line += "（情绪: \(entry.emotion)）" }
            if !entry.situation.isEmpty { line += "（情境: \(entry.situation)）" }
            return line
        }.joined(separator: "\n")

        return """
        请分析以下自动想法列表，找出其中最常见的认知扭曲模式，并总结整体倾向：

        \(thoughtsList)
        """
    }

    /// 与 `RiskLexicon` 对齐：高风险时用于界面提示（如安全横幅）。
    static func containsCrisisContent(_ text: String) -> Bool {
        detectRiskLevel(text) == .high
    }
}
