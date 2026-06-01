import XCTest

final class CBTReframeUITests: XCTestCase {

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    override func tearDownWithError() throws {
        // Put teardown code here.
    }

    @MainActor
    func testAnalyzeThoughtAndSave() throws {
        // UI tests must launch the application that they test.
        let app = XCUIApplication()
        
        // Inject UI_TESTING environment variable to use MockAIService
        app.launchEnvironment["UI_TESTING"] = "1"
        app.launch()

        // Wait for app to be ready (look for some initial element)
        // Adjust the identifier/text based on the actual UI.
        
        // 1. Type "今天心情很糟"
        // Let's look for a text view or text field. 
        // We will tap the text editor, type the text.
        // If there's an explicit accessibility identifier, use it, otherwise find the first TextEditor/TextField.
        
        // Find the main text editor (using a heuristic that it's a TextView)
        let textView = app.textViews.firstMatch
        XCTAssertTrue(textView.waitForExistence(timeout: 5.0), "The thought input text view should exist.")
        textView.tap()
        textView.typeText("今天心情很糟")

        // 2. Tap through the flow
        let nextStep1Button = app.buttons["下一步：选最省力的方式"]
        if nextStep1Button.waitForExistence(timeout: 2.0) {
            nextStep1Button.tap()
        }
        
        let nextStep2Button = app.buttons["下一步：点当前心情"]
        if nextStep2Button.waitForExistence(timeout: 2.0) {
            nextStep2Button.tap()
        }
        
        let analyzeButton = app.buttons["开始分析"]
        if analyzeButton.waitForExistence(timeout: 2.0) {
            analyzeButton.tap()
        } else {
            // Fallback: try to tap the first button containing "分析"
            let predicate = NSPredicate(format: "label CONTAINS[c] '分析'")
            let fallbackBtn = app.buttons.containing(predicate).firstMatch
            if fallbackBtn.exists { fallbackBtn.tap() }
        }

        // 3. Wait for mock -> Assert no EXC_BREAKPOINT and verify the success UI.
        // Mock takes 1 second.
        // We expect to see "灾难化思维 (Mock)"
        let resultText = app.staticTexts["灾难化思维 (Mock)"]
        let exists = resultText.waitForExistence(timeout: 5.0)
        
        XCTAssertTrue(exists, "The mock analysis result should be displayed, indicating no crash and success UI.")
    }
}
