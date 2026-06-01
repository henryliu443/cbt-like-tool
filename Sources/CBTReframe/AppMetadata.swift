import Foundation

/// 与 Xcode `MARKETING_VERSION` / `CURRENT_PROJECT_VERSION` 同步，供界面与导出元数据使用。
enum AppMetadata {
    static var marketingVersion: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "0"
    }

    static var buildNumber: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "0"
    }

    /// 例：`2.1 (21)`
    static var versionLabel: String {
        "\(marketingVersion) (\(buildNumber))"
    }
}
