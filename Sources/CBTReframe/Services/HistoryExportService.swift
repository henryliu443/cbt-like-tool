#if !SKIP
import Foundation
#if os(iOS)
import UIKit
#endif

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
            try data.write(to: url, options: Data.WritingOptions.atomic)
            return url
        } catch {
            return nil
        }
    }

    static func makeTemporaryCSVFile(entries: [HistoryEntry]) -> URL? {
        let header = "createdAt,inputThought,moodTag,template,distortion,alternative,action,provider,model,isFavorite\n"
        let rows = entries.map { e in
            [
                isoDate(e.createdAt),
                csvEscape(e.inputThought),
                csvEscape(e.moodTag),
                csvEscape(e.therapyTemplateRaw),
                csvEscape(e.distortion),
                csvEscape(e.alternative),
                csvEscape(e.action),
                csvEscape(e.providerName),
                csvEscape(e.modelName),
                e.isFavorite ? "1" : "0",
            ].joined(separator: ",")
        }.joined(separator: "\n")
        let content = header + rows
        let name = "CBTReframe-History-\(Int(Date().timeIntervalSince1970)).csv"
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(name)
        do {
            try content.write(to: url, atomically: true, encoding: .utf8)
            return url
        } catch {
            return nil
        }
    }

    static func makeTemporaryPDFFile(entries: [HistoryEntry]) -> URL? {
        #if os(iOS)
        let meta: [CFString: Any] = [kCGPDFContextCreator: "CBTReframe", kCGPDFContextTitle: "History Export"]
        let format = UIGraphicsPDFRendererFormat()
        format.documentInfo = meta as [String: Any]
        let bounds = CGRect(x: 0, y: 0, width: 595, height: 842)
        let renderer = UIGraphicsPDFRenderer(bounds: bounds, format: format)
        let data = renderer.pdfData { ctx in
            ctx.beginPage()
            let paragraph = NSMutableParagraphStyle()
            paragraph.lineSpacing = 4
            let attrs: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 12),
                .paragraphStyle: paragraph,
            ]
            var y: CGFloat = 20
            for (i, entry) in entries.enumerated() {
                let line = "\(i + 1). [\(isoDate(entry.createdAt))] \(entry.inputThought)\n扭曲: \(entry.distortion)\n替代: \(entry.alternative)\n行动: \(entry.action)\n\n"
                let rect = CGRect(x: 20, y: y, width: 555, height: 140)
                line.draw(in: rect, withAttributes: attrs)
                y += 130
                if y > 760 {
                    ctx.beginPage()
                    y = 20
                }
            }
        }
        let name = "CBTReframe-History-\(Int(Date().timeIntervalSince1970)).pdf"
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(name)
        do {
            try data.write(to: url)
            return url
        } catch {
            return nil
        }
        #else
        _ = entries
        return nil
        #endif
    }

    private static func csvEscape(_ value: String) -> String {
        "\"\(value.replacingOccurrences(of: "\"", with: "\"\""))\""
    }

    private static func isoDate(_ date: Date) -> String {
        let f = ISO8601DateFormatter()
        return f.string(from: date)
    }
}

#else
import Foundation

enum HistoryExportService {
    static func makeTemporaryJSONFile(entries: [HistoryEntry]) -> URL? { return nil }
    static func makeTemporaryCSVFile(entries: [HistoryEntry]) -> URL? { return nil }
    static func makeTemporaryPDFFile(entries: [HistoryEntry]) -> URL? { return nil }
}
#endif
