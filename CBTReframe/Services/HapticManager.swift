import Foundation
#if !SKIP
import UIKit
#endif

final class HapticManager {
    static let shared = HapticManager()
    
    private init() {}
    
    func notification(type: HapticType) {
        #if !SKIP
        let generator = UINotificationFeedbackGenerator()
        switch type {
        case .success:
            generator.notificationOccurred(.success)
        case .warning:
            generator.notificationOccurred(.warning)
        case .error:
            generator.notificationOccurred(.error)
        }
        #endif
    }
    
    func impact(style: ImpactStyle) {
        #if !SKIP
        let uiStyle: UIImpactFeedbackGenerator.FeedbackStyle
        switch style {
        case .light: uiStyle = .light
        case .medium: uiStyle = .medium
        case .heavy: uiStyle = .heavy
        case .soft: uiStyle = .soft
        case .rigid: uiStyle = .rigid
        }
        let generator = UIImpactFeedbackGenerator(style: uiStyle)
        generator.impactOccurred()
        #endif
    }
    
    enum HapticType {
        case success
        case warning
        case error
    }
    
    enum ImpactStyle {
        case light
        case medium
        case heavy
        case soft
        case rigid
    }
}
