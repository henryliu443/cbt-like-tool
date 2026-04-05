import Foundation

/// V2.1：将历史记录导出为 JSON，便于备份或在外部工具中查看。
enum HistoryExportService {
    private static let schemaVersion = 1

    struct Envelope: Codable {
        let schemaVersion: Int
        let appMarketingVersion: String
        let appBuild: String
        let exportedAt: Date
        let entryCount: Int
        let entries: [Row]
    }

    struct Row: Codable {
        let id: String
        let createdAt: Date
        let inputThought: String
        let moodTag: String
        let therapyTemplateRaw: String
        let analysisDepthRaw: String
        let responseStyleRaw: String
        let distortion: String
        let alternative: String
        let action: String
        let isFavorite: Bool
        let providerName: String
        let modelName: String
        let resultExtrasJSON: String
    }

    static func row(from entry: HistoryEntry) -> Row {
        Row(
            id: entry.id.uuidString,
            createdAt: entry.createdAt,
            inputThought: entry.inputThought,
            moodTag: entry.moodTag,
            therapyTemplateRaw: entry.therapyTemplateRaw,
            analysisDepthRaw: entry.analysisDepthRaw,
            responseStyleRaw: entry.responseStyleRaw,
            distortion: entry.distortion,
            alternative: entry.alternative,
            action: entry.action,
            isFavorite: entry.isFavorite,
            providerName: entry.providerName,
            modelName: entry.modelName,
            resultExtrasJSON: entry.resultExtrasJSON
        )
    }

    static func makeEnvelope(entries: [HistoryEntry]) -> Envelope {
        Envelope(
            schemaVersion: Self.schemaVersion,
            appMarketingVersion: AppMetadata.marketingVersion,
            appBuild: AppMetadata.buildNumber,
            exportedAt: Date(),
            entryCount: entries.count,
            entries: entries.map { row(from: $0) }
        )
    }

    /// 写入临时目录并返回文件 URL，供 `ShareLink` 使用。
    static func makeTemporaryJSONFile(entries: [HistoryEntry]) -> URL? {
        let envelope = makeEnvelope(entries: entries)
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        encoder.dateEncodingStrategy = .iso8601
        guard let data = try? encoder.encode(envelope) else { return nil }
        let name = "CBTReframe-History-\(Int(Date().timeIntervalSince1970)).json"
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(name)
        do {
            try data.write(to: url, options: .atomic)
            return url
        } catch {
            return nil
        }
    }
}
