enum PromptTemplate { case cbtReframe }
enum ReframeMode { case quick }
enum ResponseStyle { case brief }

struct PromptBuilder {
    static let akathisiaSafetyInstructions = """
    【Akathisia 补充】
    用户标记了静坐不能（Akathisia）。请遵守：
    - 承认其不安可能部分由生理因素驱动，不必把一切不适都归为「认知」或「性格」。
    - 避免过度认知解释与长篇扭曲分析；不要替用户下绝对化结论。
    - 通篇多用「可能」「似乎」「不一定」等不确定表述，少用断定句式。
    """

    static func buildSystemPrompt(hasAkathisia: Bool) -> String {
        let roleIntro = "你是一位专业的认知行为治疗（CBT）辅助工具。你温和、有同理心、专业。"
        let templateInstructions = "请对用户的想法进行认知行为治疗式的分析："
        let modeInstructions = "请简洁回复，每部分1-2句话。"
        let styleInstructions = "风格要求：直接、简洁、专业。"
        let outputFormat = "【输出要求】请严格按照以下 JSON 格式输出。"

        let akathisiaBlock = hasAkathisia
            ? "\n\n\(akathisiaSafetyInstructions)\n"
            : ""

        return """
        \(roleIntro)

        \(templateInstructions)

        \(modeInstructions)
        \(styleInstructions)
        \(akathisiaBlock)
        \(outputFormat)
        """
    }
}
print("--- hasAkathisia: false ---")
print(PromptBuilder.buildSystemPrompt(hasAkathisia: false))
print("--- hasAkathisia: true ---")
print(PromptBuilder.buildSystemPrompt(hasAkathisia: true))
