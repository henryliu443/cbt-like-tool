fun main() {
    val roleIntro = "你是一位专业的认知行为治疗（CBT）辅助工具。你温和、有同理心、专业。"
    val templateInstructions = """
请对用户的想法进行认知行为治疗式的分析：
1. 识别其中可能存在的认知扭曲类型
2. 提供一个更平衡、更理性的替代想法
3. 建议一个具体的小行动来帮助用户
""".trim()
    val modeInstructions = "请简洁回复，每部分1-2句话。"
    val styleInstructions = "风格要求：直接、简洁、专业。"
    val outputFormat = "【输出要求】请严格按照以下 JSON 格式输出。"

    val akathisiaSafetyInstructions = """
    【Akathisia 补充】
    用户标记了静坐不能（Akathisia）。请遵守：
    - 承认其不安可能部分由生理因素驱动，不必把一切不适都归为「认知」或「性格」。
    - 避免过度认知解释与长篇扭曲分析；不要替用户下绝对化结论。
    - 通篇多用「可能」「似乎」「不一定」等不确定表述，少用断定句式。
    """.trimIndent()

    fun build(hasAkathisia: Boolean): String {
        val akathisiaBlock = if (hasAkathisia) {
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
""".trimIndent()
        if (akathisiaBlock.isEmpty()) {
            return result.replace("$styleInstructions\n\n\n$outputFormat", "$styleInstructions\n\n$outputFormat")
        }
        return result
    }
    
    println("--- hasAkathisia: false ---")
    println(build(false))
    println("--- hasAkathisia: true ---")
    println(build(true))
}
