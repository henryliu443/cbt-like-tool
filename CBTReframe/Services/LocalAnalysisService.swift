import Foundation

struct LocalAnalysisService: AIServiceProtocol {
    let provider = AIProvider.local

    func reframe(
        thought: String,
        model: AIModel,
        mode: ReframeMode,
        style: ResponseStyle,
        template: PromptTemplate
    ) async throws -> AnalysisResult {
        try await Task.sleep(nanoseconds: 500_000_000)

        let trimmed = thought.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            return AnalysisResult(
                distortion: "无法分析",
                alternative: "请先输入一个想法",
                action: "试着写下你的感受"
            )
        }

        let pool: [AnalysisResult]
        switch template {
        case .cbtReframe:
            pool = Self.cbtPool
        case .socratic:
            pool = Self.socraticPool
        case .behavioral:
            pool = Self.behavioralPool
        }

        let index = Self.stableIndex(for: trimmed, count: pool.count)
        return pool[index]
    }

    func analyzeThoughtPatterns(
        thoughts: [ThoughtEntry],
        model: AIModel
    ) async throws -> ThoughtPatternReport {
        try await Task.sleep(nanoseconds: 300_000_000)

        let grouped = Dictionary(grouping: thoughts) { entry in
            Self.detectDistortion(for: entry.content)
        }

        let sortedGroups = grouped
            .map { name, entries in
                ThoughtPatternReport.DistortionCount(
                    name: name,
                    count: entries.count,
                    example: entries.first?.content ?? ""
                )
            }
            .sorted { lhs, rhs in
                if lhs.count == rhs.count {
                    return lhs.name < rhs.name
                }
                return lhs.count > rhs.count
            }

        let topDistortions = Array(sortedGroups.prefix(3))
        let overallPattern = topDistortions.isEmpty
            ? "最近记录还不够多，暂时看不出稳定模式。"
            : "你最近更常出现的模式是\(topDistortions.map(\.name).joined(separator: "、"))。这些想法容易在情绪上来时快速放大压力。"
        let suggestion = topDistortions.contains(where: { $0.name == "灾难化思维" })
            ? "先把“最坏结果”和“最可能结果”分开写下来，再决定下一步。"
            : "下次记录想法时，补一句证据支持或不支持它，可以更快打断自动化反应。"

        return ThoughtPatternReport(
            topDistortions: topDistortions,
            overallPattern: overallPattern,
            suggestion: suggestion
        )
    }

    private static func detectDistortion(for thought: String) -> String {
        let text = thought.lowercased()

        if text.contains("一定") || text.contains("完了") || text.contains("最糟") {
            return "灾难化思维"
        }

        if text.contains("都") || text.contains("从来") || text.contains("永远") {
            return "过度概括"
        }

        if text.contains("应该") || text.contains("必须") {
            return "应该思维"
        }

        if text.contains("他们觉得") || text.contains("别人肯定") {
            return "读心术"
        }

        return "情绪化推理"
    }

    private static func stableIndex(for text: String, count: Int) -> Int {
        let hash = text.unicodeScalars.reduce(into: UInt64(5381)) { partialResult, scalar in
            partialResult = ((partialResult << 5) &+ partialResult) &+ UInt64(scalar.value)
        }
        return Int(hash % UInt64(count))
    }

    private static let cbtPool: [AnalysisResult] = [
        AnalysisResult(
            distortion: "灾难化思维",
            alternative: "事情不一定会变得最糟，过去类似的情况我也度过了。试着想一想最可能发生的结果，而不是最坏的。",
            action: "先喝一口水，暂停一下，写下三个最可能的结果"
        ),
        AnalysisResult(
            distortion: "非黑即白思维",
            alternative: "大多数事情都存在中间地带，不是全对或全错。也许这件事有好有坏，两者并存。",
            action: "写下这件事中一个还不错的部分"
        ),
        AnalysisResult(
            distortion: "过度概括",
            alternative: "一次失败不代表永远失败，每次经历都不同。这只是众多经历中的一个。",
            action: "回忆一次你成功应对类似情况的经历"
        ),
        AnalysisResult(
            distortion: "读心术",
            alternative: "我无法确定别人在想什么，也许他们根本没注意到。我们常常高估别人对我们的关注程度。",
            action: "下次直接友善地问对方怎么想"
        ),
        AnalysisResult(
            distortion: "情绪化推理",
            alternative: "感觉糟糕不等于事实糟糕，情绪会过去的。当前的感受不能代替客观分析。",
            action: "做三次深呼吸，感受一下身体的变化"
        ),
        AnalysisResult(
            distortion: "应该思维",
            alternative: "「应该」和「必须」会制造不必要的压力。试着用「我希望」或「如果能就好了」替代。",
            action: "把你脑中的「应该」改写成「我想要」，感受一下有什么不同"
        ),
        AnalysisResult(
            distortion: "贴标签",
            alternative: "你不是你做的某一件事。一次表现不代表你这个人。行为和身份是不同的。",
            action: "写下三个与这个标签矛盾的事实"
        ),
    ]

    private static let socraticPool: [AnalysisResult] = [
        AnalysisResult(
            distortion: "值得深入探索的想法",
            alternative: "这个想法有什么证据支持？有什么证据不支持？如果你的好朋友有这个想法，你会怎么跟TA说？",
            action: "花5分钟用纸笔分别写下「支持」和「不支持」这个想法的证据"
        ),
        AnalysisResult(
            distortion: "可能存在认知偏差",
            alternative: "你这个想法的确定程度有多高（0-100%）？如果降低到50%，事情会有什么不同？一年后你会怎么看这件事？",
            action: "给这个想法打一个确信度分数，明天再打一次，对比一下"
        ),
        AnalysisResult(
            distortion: "情绪驱动的判断",
            alternative: "这是一个事实还是一种感觉？如果你心情很好的时候，会怎么看这件事？你有没有忽略什么信息？",
            action: "把这个想法写下来，然后假装是别人写的，客观地分析它"
        ),
    ]

    private static let behavioralPool: [AnalysisResult] = [
        AnalysisResult(
            distortion: "回避模式",
            alternative: "当我们感到不好时，容易想要逃避。但小步行动往往能打破消极循环。",
            action: "选一件你一直在拖延的小事，现在花2分钟开始做"
        ),
        AnalysisResult(
            distortion: "消极循环",
            alternative: "情绪和行为互相影响。改变行为可以慢慢改善情绪。",
            action: "站起来做10个伸展动作，然后喝一杯水"
        ),
        AnalysisResult(
            distortion: "精力耗竭",
            alternative: "你可能忘了给自己充电。愉悦活动和掌控感活动都很重要。",
            action: "列出3件今天能让你开心的小事，选一件现在就做"
        ),
    ]
}
