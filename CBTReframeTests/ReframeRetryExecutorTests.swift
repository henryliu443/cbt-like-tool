import XCTest
@testable import CBTReframe

final class ReframeRetryExecutorTests: XCTestCase {
    func testRetriesOnceAndSucceeds() async throws {
        var callCount = 0

        let result = try await ReframeRetryExecutor.run(maxAttempts: 2, retryDelayNanoseconds: 0) {
            callCount += 1
            if callCount == 1 {
                throw AIServiceError.httpStatus(503)
            }
            return "ok"
        }

        XCTAssertEqual(callCount, 2)
        XCTAssertEqual(result.value, "ok")
        XCTAssertEqual(result.attemptCount, 2)
        XCTAssertTrue(result.recoveredByRetry)
    }

    func testStopsAfterTwoAttemptsWhenStillFailing() async {
        var callCount = 0

        do {
            _ = try await ReframeRetryExecutor.run(maxAttempts: 2, retryDelayNanoseconds: 0) {
                callCount += 1
                throw AIServiceError.httpStatus(503)
            }
            XCTFail("Expected failure")
        } catch let error as AIServiceError {
            XCTAssertEqual(callCount, 2)
            guard case .httpStatus(let code) = error else {
                XCTFail("Expected HTTP status error")
                return
            }
            XCTAssertEqual(code, 503)
        } catch {
            XCTFail("Unexpected error: \(error)")
        }
    }

    func testDoesNotRetryNonRetriableError() async {
        var callCount = 0

        do {
            _ = try await ReframeRetryExecutor.run(maxAttempts: 2, retryDelayNanoseconds: 0) {
                callCount += 1
                throw AIServiceError.invalidKey
            }
            XCTFail("Expected invalidKey")
        } catch let error as AIServiceError {
            XCTAssertEqual(callCount, 1)
            guard case .invalidKey = error else {
                XCTFail("Expected invalidKey")
                return
            }
        } catch {
            XCTFail("Unexpected error: \(error)")
        }
    }
}
