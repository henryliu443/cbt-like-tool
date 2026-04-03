import UIKit

/// 外站 AI：优先打开已安装客户端（`canOpenURL`），否则用网页。
enum ExternalAIAppLauncher {

    static func openChatGPT() {
        openPreferringApp(
            appCandidates: [
                URL(string: "chatgpt://"),
            ],
            fallback: URL(string: "https://chat.openai.com")!
        )
    }

    static func openDeepSeek() {
        openPreferringApp(
            appCandidates: [
                URL(string: "deepseek://"),
                URL(string: "deepseek://chat"),
            ],
            fallback: URL(string: "https://chat.deepseek.com")!
        )
    }

    static func openGemini() {
        openPreferringApp(
            appCandidates: [
                URL(string: "googlegemini://"),
                URL(string: "gemini://"),
                URL(string: "googleapp://"),
            ],
            fallback: URL(string: "https://gemini.google.com")!
        )
    }

    static func openKimi() {
        openPreferringApp(
            appCandidates: [
                URL(string: "kimi://"),
                URL(string: "moonshot://"),
            ],
            fallback: URL(string: "https://kimi.moonshot.cn")!
        )
    }

    private static func openPreferringApp(appCandidates: [URL?], fallback: URL) {
        let app = UIApplication.shared
        let urls = appCandidates.compactMap { $0 }
        if let chosen = urls.first(where: { app.canOpenURL($0) }) {
            app.open(chosen)
        } else {
            app.open(fallback)
        }
    }
}
