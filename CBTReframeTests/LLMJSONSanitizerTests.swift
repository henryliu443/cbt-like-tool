import XCTest
@testable import CBTReframe

final class LLMJSONSanitizerTests: XCTestCase {
    func testSanitizeFenceAndKeepObject() {
        let raw = """
        ```json
        {"distortion":"灾难化","alternative":"还有其他可能","action":"先深呼吸"}
        ```
        """
        let out = LLMJSONSanitizer.sanitizeForJSONObject(raw)
        XCTAssertTrue(out.contains("\"distortion\""))
        XCTAssertFalse(out.contains("```"))
    }
}
