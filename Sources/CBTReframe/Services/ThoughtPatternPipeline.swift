import Foundation

@MainActor
final class ThoughtPatternPipeline {
    private let resolver: AIProviderResolver

    init(resolver: AIProviderResolver) {
        self.resolver = resolver
    }

    func analyze(entries: [ThoughtEntry]) async throws -> ThoughtPatternReport {
        let service = AIServiceFactory.service(for: resolver.selectedProvider)
        return try await service.analyzeThoughtPatterns(
            thoughts: entries,
            model: resolver.selectedModel
        )
    }
}
