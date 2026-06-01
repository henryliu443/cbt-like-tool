import Foundation
import OSLog

enum AppLogger {
    static let network = Logger(subsystem: "com.cbt.reframe", category: "network")
    static let data = Logger(subsystem: "com.cbt.reframe", category: "data")
}
