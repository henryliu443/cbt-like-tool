# Handoff: iOS Native Exercise Interface Analysis

## Overview
This report details the investigation of the iOS project structure, KMP shared logic, and the requirements for the `ExerciseSession` redesign.

## Observation
1. **KMP Setup & SKIE:** A scan of `shared/build.gradle.kts` and `libs.versions.toml` (or lack thereof) confirms that `SKIE` or `KMPNativeCoroutines` is **not** currently used in the project. The KMP framework is not actively imported into `CBTReframeApp.swift` yet.
2. **StateFlow:** The `ExerciseSessionViewModel` exposes `StateFlow<ExerciseSessionState>` natively (`val state: StateFlow<ExerciseSessionState>`).
3. **Xcode Project:** `CBTReframe.xcodeproj/project.pbxproj` does not show a `WidgetExtension` target (searched for "WidgetExtension").
4. **App Wiring:** `CBTReframeApp.swift` has a `private struct ExercisesView` providing a simple list of breathing exercises routing to `ExerciseGuideView`.

## Logic Chain
- **StateFlow Subscription Strategy:** Because the project lacks SKIE, bridging `StateFlow` to Swift requires either adding SKIE to the `shared/build.gradle.kts` (recommended for seamless `AsyncSequence` support) OR writing an `@Observable` wrapper class in Swift that interacts with a custom Kotlin `FlowCollector` callback function. Out-of-the-box KMP `StateFlow` maps to an Obj-C generic `Kotlinx_coroutines_coreStateFlow` which is difficult to observe directly in SwiftUI.
- **Xcode Modification Strategy:** Modifying `CBTReframe.xcodeproj` programmatically is best achieved via the Ruby `xcodeproj` gem. This can reliably create groups (`Haptic`, `Views/Exercises`, `LiveActivity`) and add the 4 Swift files to the main target's source build phase.
- **Live Activity Strategy:** `ExerciseLiveActivity.swift` uses `ActivityKit`. In Xcode, Live Activities *must* be housed within a Widget Extension target to function on the Lock Screen/Dynamic Island. Since no Widget target exists, simply adding the file to the `CBTReframe` target will cause a compiler or runtime failure.
- **App Wiring Strategy:** `ExercisesView` inside `CBTReframeApp.swift` needs to be updated. A NavigationLink should route to the new `ExerciseSessionView` (instantiated with a newly created `ExerciseSessionViewModel`) instead of the hardcoded `ExerciseGuideView`.

## Caveats
- Adding a Widget Extension target programmatically via a script is highly complex (involves generating `Info.plist`, build phases, and code signing). It is strongly advised to add the Widget Extension manually via Xcode (`File -> New -> Target -> Widget Extension`) before attempting to use the Ruby script to insert `ExerciseLiveActivity.swift` into it.
- KMP compilation to iOS must be fully configured (via CocoaPods or SPM/Run Script) to allow Swift to import `shared`. This might be missing from the iOS project given the current `.pbxproj`.

## Conclusion
1. **Swift StateFlow Subscription:** Add `SKIE` to KMP or wrap the Kotlin `StateFlow` with a callback. Swift should use an `@Observable` class to map the KMP state into SwiftUI.
2. **Xcode Script:** Write a Ruby script utilizing the `xcodeproj` gem to add `AdvancedHapticEngine.swift`, `FluidBreathingRenderer.swift`, and `ExerciseSessionView.swift` to the `CBTReframe` target.
3. **Live Activity:** **Stop and create** a `CBTReframeWidgetExtension` target manually in Xcode before trying to implement `ExerciseLiveActivity.swift`. Then add the file to that specific target.
4. **Wiring:** Inject `ExerciseSessionView` into `CBTReframeApp.swift`'s `ExercisesView` navigation stack.

## Verification Method
- **StateFlow:** Verify compilation in Xcode after importing the KMP framework.
- **Live Activity:** Run on an iOS Simulator (iOS 16.2+) and trigger the Live Activity; check if it appears in Dynamic Island.
- **Xcode Script:** Run the Ruby script and open `CBTReframe.xcodeproj` in Xcode; confirm no project corruption and files appear under their respective groups.
