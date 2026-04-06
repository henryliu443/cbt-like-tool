import XCTest
@testable import CBTReframe

final class RiskRoutingTests: XCTestCase {
    func testImmediateCrisisKeywordDetected() {
        XCTAssertEqual(detectRiskLevel("我想结束生命"), .high)
        XCTAssertTrue(shouldUseLocalCrisisOnly("kill myself"))
    }

    func testMediumRiskFallsToGentle() {
        let level = detectRiskLevel("我最近很累，感觉快撑不住了")
        XCTAssertEqual(level, .medium)
        XCTAssertEqual(routeStrategy(level: level), .cbtGentle)
    }
}
