import XCTest
@testable import CBTReframe

final class PromptBuilderTests: XCTestCase {
    func testPromptContainsRequiredKeys() {
        let prompt = PromptBuilder.buildSystemPrompt(
            mode: .balanced,
            style: .warm,
            template: .cbtReframe,
            strategy: .cbtNormal,
            mood: "焦虑",
            hasAkathisia: false
        )
        XCTAssertTrue(prompt.contains("distortion"))
        XCTAssertTrue(prompt.contains("alternative"))
        XCTAssertTrue(prompt.contains("action"))
    }
}
