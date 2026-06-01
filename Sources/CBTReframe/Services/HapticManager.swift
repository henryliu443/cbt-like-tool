import Foundation
#if os(iOS) && !SKIP
import UIKit
#endif

#if SKIP
import android.content.Context
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
#endif

final class HapticManager {
    static let shared = HapticManager()
    
    private init() {}

    static func success() {
        shared.notification(type: .success)
    }
    
    func notification(type: HapticType) {
        #if os(iOS) && !SKIP
        let generator = UINotificationFeedbackGenerator()
        switch type {
        case .success:
            generator.notificationOccurred(.success)
        case .warning:
            generator.notificationOccurred(.warning)
        case .error:
            generator.notificationOccurred(.error)
        }
        #elseif SKIP
        guard let context = ProcessInfo.processInfo.androidContext as? android.content.Context,
              let vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator,
              vibrator.hasVibrator() else { return }
              
        if Build.VERSION.SDK_INT >= Build.VERSION_CODES.O {
            let effect: VibrationEffect
            switch type {
            case .success:
                effect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            case .warning:
                effect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
            case .error:
                effect = VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            vibrator.vibrate(effect)
        } else {
            vibrator.vibrate(50)
        }
        #endif
    }
    
    func impact(style: ImpactStyle) {
        #if os(iOS) && !SKIP
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
        #elseif SKIP
        guard let context = ProcessInfo.processInfo.androidContext as? android.content.Context,
              let vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator,
              vibrator.hasVibrator() else { return }
              
        if Build.VERSION.SDK_INT >= Build.VERSION_CODES.O {
            let effect: VibrationEffect
            switch style {
            case .light, .soft:
                effect = VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
            case .medium:
                effect = VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
            case .heavy, .rigid:
                effect = VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            vibrator.vibrate(effect)
        } else {
            vibrator.vibrate(30)
        }
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
