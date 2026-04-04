import XCTest
@testable import CBTReframe

final class AIServiceErrorRetryPolicyTests: XCTestCase {
    func testHTTPStatusRetryPolicy() {
        XCTAssertTrue(AIServiceError.httpStatus(429).isRetriable)
        XCTAssertTrue(AIServiceError.httpStatus(500).isRetriable)
        XCTAssertTrue(AIServiceError.httpStatus(502).isRetriable)
        XCTAssertTrue(AIServiceError.httpStatus(503).isRetriable)
        XCTAssertTrue(AIServiceError.httpStatus(504).isRetriable)

        XCTAssertFalse(AIServiceError.httpStatus(400).isRetriable)
        XCTAssertFalse(AIServiceError.httpStatus(401).isRetriable)
        XCTAssertFalse(AIServiceError.httpStatus(403).isRetriable)
    }

    func testURLErrorRetryPolicy() {
        XCTAssertTrue(AIServiceError.networkError(URLError(.timedOut)).isRetriable)
        XCTAssertTrue(AIServiceError.networkError(URLError(.notConnectedToInternet)).isRetriable)
        XCTAssertTrue(AIServiceError.networkError(URLError(.networkConnectionLost)).isRetriable)
        XCTAssertFalse(AIServiceError.networkError(URLError(.unsupportedURL)).isRetriable)
    }

    func testUserFacingMessageForBadRequest() {
        XCTAssertEqual(AIServiceError.httpStatus(400).userFacingMessage, "请求参数或模型配置有误，请检查后重试")
    }

    func testInvalidStructuredOutputIsRetriable() {
        XCTAssertTrue(AIServiceError.invalidStructuredOutput("模型未返回有效内容").isRetriable)
    }
}
