import Foundation

final class EngineRouter {
    func resolve(settings: GlobalSettings) -> AnalysisEngine {
        switch settings.thinkingTemplate {
        case .cbt:
            return CBTEngine()
        case .socratic:
            return SocraticEngine()
        case .behavioral:
            return BehavioralEngine()
        }
    }
}
