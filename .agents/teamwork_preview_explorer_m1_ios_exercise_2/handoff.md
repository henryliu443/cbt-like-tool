# Handoff Report: iOS Exercise Interface Implementation Strategy

## Observation
1. **App Structure**: The iOS entry point is `CBTReframe/CBTReframeApp.swift`. The exercise routing is currently handled by `private struct ExercisesView: View` inside `CBTReframeApp.swift` (Lines 41-107), which hardcodes a list of `ExerciseGuide` items and pushes to `ExerciseGuideView`.
2. **KMP Structure**: `shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/ExerciseSessionViewModel.kt` exposes `val state: StateFlow<ExerciseSessionState> = _state.asStateFlow()`. 
3. **KMP iOS Build**: `shared/build.gradle.kts` configures `iosX64()`, `iosArm64()`, `iosSimulatorArm64()` targets, but does NOT contain a `binaries.framework { ... }` block to generate the Apple framework, nor is there a Run Script in `CBTReframe.xcodeproj/project.pbxproj` linking it.
4. **Xcode Project Setup**: `CBTReframe.xcodeproj/project.pbxproj` does not contain references to the four required Swift files yet.

## Logic Chain
1. **Modifying Xcode Project**: To automatically include `AdvancedHapticEngine.swift`, `FluidBreathingRenderer.swift`, `ExerciseSessionView.swift`, and `ExerciseLiveActivity.swift`, a Ruby script leveraging the `xcodeproj` gem is the most reliable strategy. The script should open `CBTReframe.xcodeproj`, create the necessary groups (`Haptic`, `Views/Exercises`, `LiveActivity`), add the file references, and append them to the main target's Compile Sources build phase.
2. **Subscribing to KMP StateFlow**: Because SKIE or KMP-NativeCoroutines are not configured in this project, `StateFlow` compiles to an Objective-C interface (`Kotlinx_coroutines_coreStateFlow`). To subscribe in Swift correctly, we must create a Swift class wrapping `Kotlinx_coroutines_coreFlowCollector`, implementing its `emit(value:completionHandler:)` method. `ExerciseSessionView.swift` should use an `@StateObject` or `@Observable` wrapper that initializes `ExerciseSessionViewModel()`, calls `collect` with the custom collector, and maps the emitted `ExerciseSessionState` to a Swift `@Published` or `@State` variable to trigger UI updates.
3. **Wiring the App Structure**: `CBTReframeApp.swift` contains the `ExercisesView`. We should modify `ExerciseGuideView` (or `ExercisesView` directly) to include a "Start Session" button that navigates to `ExerciseSessionView()`, injecting the KMP ViewModel.

## Caveats
- The KMP `shared` module does not currently generate an iOS framework via `binaries.framework` in `build.gradle.kts`. The implementer will either need to add this block and a Run Script to link it, or if it is handled externally (e.g. CocoaPods not yet committed), ensure `import shared` works before writing the subscription logic.
- `ExerciseLiveActivity` requires ActivityKit and needs an `Info.plist` key `NSSupportsLiveActivities` set to `YES` to function properly, which must also be configured.

## Conclusion
1. **Xcode Project**: Write a Ruby script using `xcodeproj` to link the new files to `CBTReframe.xcodeproj` to avoid manual `pbxproj` merge conflicts.
2. **KMP StateFlow**: Implement a `Kotlinx_coroutines_coreFlowCollector` wrapper class in Swift to bridge `StateFlow<ExerciseSessionState>` updates to SwiftUI state.
3. **App Wiring**: Modify `CBTReframeApp.swift` (specifically the `ExercisesView` / `ExerciseGuideView` hierarchy) to navigate to `ExerciseSessionView`. 

## Verification Method
1. Create a dummy test file `test_xcodeproj.rb` and run `ruby test_xcodeproj.rb` to verify the `.pbxproj` correctly adds the references without breaking existing targets.
2. Build the iOS app via `xcodebuild -scheme CBTReframe -destination 'platform=iOS Simulator,name=iPhone 15'` to verify compilation of the SwiftUI and ActivityKit files.
3. Ensure KMP `shared` builds correctly for iOS before linking.
