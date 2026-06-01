import Foundation

struct RetriableValidatedReframeRunner {
    static func run(
        service: AIServiceProtocol,
        request: ReframeLLMRequest,
        model: AIModel
    ) async throws -> RetryExecutionResult<AnalysisResult> {
        let maxAttempts = (request.template == .socratic && request.strategy != .crisis) ? 3 : 2
        return try await ReframeRetryExecutor.run(maxAttempts: maxAttempts) {
            var result = try await service.reframe(
                thought: request.thought,
                mood: request.mood,
                hasAkathisia: request.hasAkathisia,
                model: model,
                mode: request.mode,
                style: request.style,
                template: request.template,
                strategy: request.strategy
            )
            if request.template == .socratic && request.strategy != .crisis {
                result = try SocraticPipelineValidation.applyingSanitizedQuestions(result)
            }
            if request.strategy != .crisis {
                try ReframeOutputGate.validate(result, template: request.template)
            }
            return result
        }
    }
}
