# Handoff Report

## Observation
1. Verified `CBTReframe.xcodeproj/project.pbxproj` and found `shared.framework` is explicitly linked with a hardcoded path pointing to `shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework`.
2. Verified `ExerciseLiveActivity.swift` was added directly to the main `CBTReframe` app target instead of a dedicated Widget Extension target.
3. In `ExerciseSessionView.swift`, the initializer explicitly evaluates the view model before passing it to `StateObject`:
   ```swift
   init(definition: ExerciseDefinition) {
       let vm = ExerciseSessionViewModel(definition: definition)
       _session = StateObject(wrappedValue: ObservableExerciseSession(viewModel: vm))
   }
   ```
4. In `ExerciseSessionView.swift`, `session.startObserving()` is called during `.onAppear`, which invokes `viewModel.state.collect(collector: ...) { ... }`. During `.onDisappear`, `session.clear()` calls `viewModel.clear()` which internally executes `scope.cancel()`.
5. In `FluidBreathingRenderer.swift`, `phase` uses hardcoded Chinese strings (e.g., `if phase == "呼气"`).

## Logic Chain
1. Hardcoding the framework path to `iosSimulatorArm64` is a textbook "works on my machine" shortcut. This will instantly fail when building for a physical device (`iosArm64`) or for a Release configuration. Production KMP projects utilize a Run Script build phase or CocoaPods to resolve paths dynamically.
2. Placing a `Widget` component like `ExerciseLiveActivity` into the main app target is another compilation shortcut. While it makes the compiler happy, Live Activities strictly require a dedicated Widget Extension target to be discovered and executed by the operating system. The Live Activity will silently fail to work on a real device.
3. SwiftUI view initializers execute frequently during parent view redraws. Because `let vm = ...` is evaluated *before* being passed to the `StateObject(wrappedValue:)` autoclosure, a new `ExerciseSessionViewModel` (which creates a new `CoroutineScope`) is instantiated on every redraw. `StateObject` correctly preserves the original instance, meaning the newly created view models and their coroutine scopes are immediately discarded but their scopes are never cancelled, causing severe memory leaks.
4. Kotlin's `StateFlow.collect` never completes. When invoked from Swift, it runs in a Kotlin/Native background interop scope—not `viewModel.scope`. Calling `viewModel.scope.cancel()` does not cancel this collector. Because `.onAppear` can be triggered multiple times (e.g., backgrounding, tab switching), each appearance leaks a new infinite coroutine.
5. `ExercisePhase` is exported from KMP and accessible in Swift (as seen in `session.state.phase.isActive`). Depending on hardcoded display names for rendering logic is highly brittle and easily breaks with localization or text changes.

## Caveats
- No actual physical device testing was performed, but the hardcoded architecture paths guarantee failure.
- The haptic engine implementation handles basics but does not explicitly handle `scenePhase` backgrounding lifecycle, which is acceptable for a first pass but not bulletproof.

## Conclusion
**VETO**. Although the project compiles successfully via the provided verification commands, the worker took major "works on my machine" shortcuts (hardcoded `iosSimulatorArm64` paths, putting Live Activities in the main target) and introduced critical memory and coroutine leaks into the Swift/KMP interop lifecycle.

## Verification Method
1. `cat CBTReframe.xcodeproj/project.pbxproj | grep -C 5 "shared.framework"` will show the hardcoded `iosSimulatorArm64` path.
2. Review the Xcode project to confirm there is no Widget Extension target.
3. Read `ExerciseSessionView.swift` to verify the `StateObject` initialization and `collect` caller logic.
4. Read `FluidBreathingRenderer.swift` to verify the usage of hardcoded Chinese strings.
