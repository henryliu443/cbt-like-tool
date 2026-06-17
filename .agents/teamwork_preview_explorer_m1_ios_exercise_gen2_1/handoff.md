# Handoff: iOS Native Exercise Interface Redesign Fix Strategy

## Observation
I investigated the 5 VETO feedback issues across the iOS app and KMP shared module:
1. **Critical Coroutine Leak**: `ExerciseSessionView.swift` calls `viewModel.state.collect(collector: collector) { err in ... }` in `ObservableExerciseSession.startObserving()`. The collection is not cancelled when `.onDisappear` triggers.
2. **SwiftUI Anti-Pattern**: In `ExerciseSessionView.swift`, `ExerciseSessionViewModel(definition: definition)` is instantiated eagerly in `init`, then passed to `StateObject(wrappedValue:)`.
3. **Brittle Localization**: `FluidBreathingRenderer.swift` accepts `phase: String` and conditionally checks hardcoded string values like `"呼气"`. `ExerciseSessionView.swift` passes `session.state.phase.displayName`. `ExercisePhase.kt` exposes an `ExercisePhase` enum with cases like `INHALE`, `HOLD`, `EXHALE`, `REST`.
4. **Hardcoded KMP Path**: `CBTReframe.xcodeproj/project.pbxproj` hardcodes the framework search path to `$(SRCROOT)/shared/build/bin/iosSimulatorArm64/debugFramework`. There is no Run Script phase for `embedAndSignAppleFrameworkForXcode`.
5. **Widget Extension**: `update_pbxproj.rb` configures `ExerciseLiveActivity.swift` under the main `CBTReframe` target instead of a dedicated ActivityKit widget extension target.

## Logic Chain
1. **Critical Coroutine Leak**: `collect` is an infinite suspending function. Without cancellation, the coroutine will continue running in the background, updating the view model and leaking memory. Wrapping the `collect` call in a Swift `Task` and calling `Task.cancel()` on `onDisappear` fixes this using native Swift concurrency.
2. **SwiftUI Anti-Pattern**: `StateObject(wrappedValue:)` uses an `@autoclosure`. Eagerly evaluating the view model before passing it circumvents the laziness of `@autoclosure`, causing the view model to be re-instantiated on every recomposition. Moving the instantiation directly into the argument defers execution correctly.
3. **Brittle Localization**: Passing raw display strings to the renderer breaks when languages change. Passing the `ExercisePhase` KMP enum provides type safety and removes brittle string comparisons.
4. **Hardcoded KMP Path**: Hardcoding `iosSimulatorArm64` breaks builds for physical devices (`iosArm64`). Using a Run Script phase with `./gradlew :shared:embedAndSignAppleFrameworkForXcode` and pointing Xcode's `FRAMEWORK_SEARCH_PATHS` to the generated `xcode-frameworks` directory enables multi-architecture builds.
5. **Widget Extension**: ActivityKit widgets require a dedicated App Extension target with the `com.apple.widgetkit-extension` point. Leaving it in the main app target causes it to fail at runtime/build-time.

## Caveats
- I assumed that the KMP project generates Swift `async` bridges for suspend functions. If it doesn't, `collect(collector:completionHandler:)` wrapped in a Task might require manual bridging with `withTaskCancellationHandler` or alternatively we return a `Closeable` handle from Kotlin. Recommending the Swift `Task` approach should handle standard KMP configurations natively.
- Writing a Ruby script for Xcodeproj to correctly create an App Extension from scratch requires precise knowledge of Build Phases and Info.plist configurations. The implementer should ensure all Info.plist keys are properly set for the new extension.

## Conclusion
The implementer should execute the following strategy:
1. **Issue 1**: In `ObservableExerciseSession`, add `private var collectionTask: Task<Void, Never>?`. Inside `startObserving()`, wrap the collection: `collectionTask = Task { try? await viewModel.state.collect(collector: collector) }`. Add `stopObserving()` to cancel the task and call it on `.onDisappear`.
2. **Issue 2**: In `ExerciseSessionView.init`, refactor to `_session = StateObject(wrappedValue: ObservableExerciseSession(viewModel: ExerciseSessionViewModel(definition: definition)))`.
3. **Issue 3**: In `FluidBreathingRenderer`, update `phase: String` to `phase: ExercisePhase`. Replace `phase == "呼气"` with `phase == .exhale`, etc. Update `ExerciseSessionView` to pass `session.state.phase`.
4. **Issue 4**: Update `.pbxproj` via a ruby script to add a "Run Script" build phase executing `cd "$SRCROOT" && ./gradlew :shared:embedAndSignAppleFrameworkForXcode`. Update `FRAMEWORK_SEARCH_PATHS` to `$(SRCROOT)/shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)` and update `shared.framework` reference.
5. **Issue 5**: Write a ruby script using the `xcodeproj` gem to create a new `:app_extension` target named `ExerciseWidgetExtension`. Move `ExerciseLiveActivity.swift` from `CBTReframe` to the new target, and add an Embed App Extensions build phase to the main app target.

## Verification Method
- **Issues 1-3**: Run Xcode build and check for compile errors. Place a print statement in `ObservableExerciseSession.deinit` to verify no memory leaks occur after navigating away.
- **Issues 4-5**: Open `.pbxproj` with Xcode or `xcodeproj` CLI, verify `ExerciseWidgetExtension` target exists and `FRAMEWORK_SEARCH_PATHS` points to the `xcode-frameworks` directory. Build the app for `Any iOS Device` (arm64) to ensure no simulator hardcoding blocks compilation.
