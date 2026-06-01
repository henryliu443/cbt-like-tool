import Foundation

struct MockAIService: AIServiceProtocol {
    var provider: AIProvider {
        // Since we are mocking, we can just return .local or whatever is available, 
        // or if AIProvider has a .mock, return that. We will use .local as a fallback.
        return .local
    }

    func reframe(
        thought: String,
        mood: String,
        hasAkathisia: Bool,
        model: AIModel,
        mode: ReframeMode,
        style: ResponseStyle,
        template: PromptTemplate,
        strategy: ResponseStrategy
    ) async throws -> AnalysisResult {
        try await Task.sleep(nanoseconds: 1_000_000_000)
        return AnalysisResult(
            distortion: "灾难化思维 (Mock)",
            alternative: "这只是一个测试回复，并不代表真实分析。",
            action: "请继续测试"
        )
    }

    func analyzeThoughtPatterns(
        thoughts: [ThoughtEntry],
        model: AIModel
    ) async throws -> ThoughtPatternReport {
        try await Task.sleep(nanoseconds: 1_000_000_000)
        return ThoughtPatternReport(
            topDistortions: [
                ThoughtPatternReport.DistortionCount(name: "Mock Distortion", count: 1, example: "Test")
            ],
            overallPattern: "Mock Pattern",
            suggestion: "Mock Suggestion"
        )
    }
}
