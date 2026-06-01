import Foundation

final class EngineRouter {
    private let cbtEngine = CBTEngine()
    private let socraticEngine = SocraticEngine()
    private let behavioralEngine = BehavioralEngine()

    func resolve(settings: GlobalSettings) -> AnalysisEngine {
        switch settings.thinkingTemplate {
        case .cbt:
            return cbtEngine
        case .socratic:
            return socraticEngine
        case .behavioral:
            return behavioralEngine
        }
    }
}
