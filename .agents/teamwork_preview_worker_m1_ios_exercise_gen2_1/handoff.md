# Handoff Report

## Observation
- `ExerciseSessionView.swift` contained a StateFlow collection that wasn't cancelled properly and eagerly evaluated `ObservableExerciseSession`.
- `FluidBreathingRenderer.swift` used a String for the exercise phase, making it brittle to localization changes.
- `project.pbxproj` contained hardcoded links to `iosSimulatorArm64` for KMP, lacking a proper Run Script phase and `FRAMEWORK_SEARCH_PATHS` configuration.
- The `ExerciseLiveActivity` widget was inside the main app target instead of a dedicated extension target.

## Logic Chain
1. **Coroutine Leak & SwiftUI Anti-Pattern**: Replaced the direct callback closure in `viewModel.state.collect` with an async/await mapping wrapped in a Swift `Task` inside `startObserving()`. The task is cancelled in `stopObserving()`, which is called during `.onDisappear` or `clear()`. The view initialization was updated to defer evaluation by passing `ObservableExerciseSession(viewModel: ...)` directly inside `StateObject(wrappedValue:)`.
2. **Brittle Localization**: Switched `FluidBreathingRenderer.swift` from taking `String` to taking the exported KMP `ExercisePhase` enum. Refactored string comparisons (e.g. `phase == "呼气"`) to enum comparisons (`phase == .exhale`).
3. **Hardcoded KMP Path**: Removed the hardcoded framework links via a Ruby script (`fix_pbxproj.rb`). Added a Run Script calling `./gradlew :shared:embedAndSignAppleFrameworkForXcode`. Set `FRAMEWORK_SEARCH_PATHS` to `$(SRCROOT)/shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`. Disabled Xcode script sandboxing to allow gradlew execution.
4. **Widget Extension**: Created the `ExerciseWidgetExtension` target via `Xcodeproj` script. Relocated `ExerciseLiveActivity.swift` to the widget target. Set `GENERATE_INFOPLIST_FILE = YES` and added `@main` to the Widget entry point. Verified the framework dependencies properly attach to the new widget target.

## Caveats
- Ensure your Xcode version fully supports Swift 5.5+ concurrency features (async/await), as this is now required to map KMP `suspend` methods properly into Swift Tasks. 

## Conclusion
All requested fixes regarding iOS redesign and architecture (VETOs 1-5) have been applied. The application safely maps KMP StateFlows, follows standard SwiftUI initialization practices, removes rigid string localization bindings, cleanly integrates the KMP compilation flow without hardcoded paths, and sets up a standard App Extension target for the live activity widget.

## Verification Method
1. Open the project in Xcode (`CBTReframe.xcodeproj`).
2. Run `xcodebuild -project CBTReframe.xcodeproj -scheme CBTReframe -sdk iphonesimulator build`
3. Verify the build succeeds without error.
4. Check that `ExerciseSessionView.swift` successfully maps `collect` using `Task { try await }` and `StateObject` no longer eagerly calculates its value.
