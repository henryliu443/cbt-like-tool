import Foundation

struct RetryExecutionResult<Value> {
    let value: Value
    let attemptCount: Int
    let recoveredByRetry: Bool
}

enum ReframeRetryExecutor {
    static func run<Value>(
        maxAttempts: Int = 2,
        retryDelayNanoseconds: UInt64 = 800_000_000,
        operation: () async throws -> Value
    ) async throws -> RetryExecutionResult<Value> {
        precondition(maxAttempts >= 1, "maxAttempts must be >= 1")

        var lastError: AIServiceError?
        for attempt in 1...maxAttempts {
            do {
                let value = try await operation()
                return RetryExecutionResult(
                    value: value,
                    attemptCount: attempt,
                    recoveredByRetry: attempt > 1
                )
            } catch {
                let serviceError = AIServiceError.classify(error)
                lastError = serviceError
                let shouldRetry = attempt < maxAttempts && serviceError.isRetriable
                if shouldRetry {
                    if retryDelayNanoseconds > 0 {
                        try? await Task.sleep(nanoseconds: retryDelayNanoseconds)
                    }
                    continue
                }
                throw serviceError
            }
        }

        throw lastError ?? AIServiceError.invalidResponse
    }
}
