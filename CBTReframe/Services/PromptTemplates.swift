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

        let outputFormat = """
        请严格按照以下 JSON 格式输出，不要包含其他内容：
        {
            "distortion": "认知扭曲类型的名称",
            "alternative": "替代想法或引导性问题",
            "action": "建议的小行动"
        }
        """

        return """
        \(roleIntro)

        \(templateInstructions)

        \(modeInstructions)
        \(styleInstructions)

        \(outputFormat)
        """
    }

    static func buildUserPrompt(thought: String) -> String {
        return "我的想法是：\(thought)"
    }

    static let crisisKeywords: Set<String> = [
        "自杀", "不想活", "死了算了", "结束生命", "跳楼", "割腕",
        "活着没意思", "去死", "了结", "轻生",
        "suicide", "kill myself", "end my life", "want to die",
    ]

    static func containsCrisisContent(_ text: String) -> Bool {
        let lowered = text.lowercased()
        return crisisKeywords.contains { lowered.contains($0) }
    }
}
