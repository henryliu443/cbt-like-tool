# Handoff Report: iOS Native Exercise Interface Strategy

## Observation
1. **KMP Integration**: The `shared/build.gradle.kts` does not contain a `binaries.framework { ... }` block for the iOS targets. Attempting to build the framework using `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` fails with `task not found`.
2. **KMP StateFlow**: `ExerciseSessionViewModel.kt` uses pure `StateFlow<ExerciseSessionState>`. The project does not use `SKIE` or `KMP-NativeCoroutines` plugins.
3. **App Structure**: `CBTReframe/Views/ExercisesView.swift` contains a `NavigationStack` with a `List` of `ExerciseRowView`s. It currently only toggles an expansion state to show text, lacking any routing to an active session.
4. **Xcode Project**: `grep "isa = PBXNativeTarget" CBTReframe.xcodeproj/project.pbxproj` reveals only `CBTReframe` and `CBTReframeTests`. There is no Widget Extension target.

## Logic Chain
1. **Subscribing to KMP StateFlow**: Because `binaries.framework` is missing, the KMP code cannot be imported into iOS yet. Once fixed, `StateFlow` will be exported to Swift as the Objective-C protocol `Kotlinx_coroutines_coreStateFlow`. To correctly subscribe from Swift without plugins, one must either write a Swift class conforming to `Kotlinx_coroutines_coreFlowCollector` to bridge the emissions to an `@Published` property, or create a Kotlin wrapper function in `shared/src/iosMain` that collects the flow and invokes a Swift closure. 
2. **Modifying CBTReframe.xcodeproj**: A Ruby script utilizing the `xcodeproj` gem can programmatically inject the 4 files into the `.pbxproj`. The script will open the project, locate the main `CBTReframe` target, create the necessary groups (e.g., `Haptic`, `Views/Exercises`, `LiveActivity`), and call `add_file_references()`.
3. **Wiring ExerciseSessionView**: In `ExercisesView.swift`, the `ExerciseRowView` expanded state should include a "Start Exercise" button wrapped in a `NavigationLink` that pushes `ExerciseSessionView` onto the existing `NavigationStack`.

## Caveats
- **Widget Extension Missing**: Adding `ExerciseLiveActivity.swift` to the main target via script will compile the `ActivityAttributes`, but iOS strictly requires a Widget Extension target to render ActivityKit UI on the Lock Screen/Dynamic Island. The implementer must add a Widget Extension via Xcode.
- **KMP Build Missing**: The `shared/build.gradle.kts` must be patched to generate the `.framework` (and Xcode build phases must be configured to embed it) before Swift can import `shared`.

## Conclusion
- **Strategy 1 (KMP)**: Patch `shared/build.gradle.kts` to output an iOS framework. Wrap `ExerciseSessionViewModel.state` collection via a Swift flow collector or an `iosMain` callback wrapper. 
- **Strategy 2 (Xcode Script)**: Use `ruby -r xcodeproj` to add `AdvancedHapticEngine.swift`, `FluidBreathingRenderer.swift`, and `ExerciseSessionView.swift` to the main target. Escalate the need to manually create a Widget Extension target for `ExerciseLiveActivity.swift`.
- **Strategy 3 (Wiring)**: Insert a `NavigationLink(destination: ExerciseSessionView(viewModel: ...))` inside the expanded details of `ExerciseRowView` in `ExercisesView.swift`.

## Verification Method
1. **KMP Build**: Add `binaries.framework { baseName="shared" }` to `shared/build.gradle.kts` and verify `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` succeeds.
2. **Xcode Script**: Run the custom ruby script, then run `xcodebuild -project CBTReframe.xcodeproj -list` or inspect the project in Xcode to ensure the files appear under the correct Target sources.
3. **Live Activity**: Test ActivityKit execution on a physical device or iOS 16.1+ simulator; it will fail to display UI until the Widget Extension is correctly configured.
