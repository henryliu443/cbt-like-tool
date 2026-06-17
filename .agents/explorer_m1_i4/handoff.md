# Handoff Report

## 1. Observation
- **Memory Leak**: I observed that `CBTReframe/Views/Exercises/ExerciseSessionView.swift` (lines 26-28) contains a `guard let self = self else { return }` statement within a `Task`. This creates a strong reference that prevents `ObservableExerciseSession` from deallocating because `self.viewModel.state.collect` runs infinitely.
- **Missing ActivityKit Logic**: I verified that `ExerciseLiveActivity` and `ExerciseAttributes` are defined in `CBTReframe/LiveActivity/ExerciseLiveActivity.swift`, but no code ever calls `Activity.request`, `Activity.update`, or `Activity.end` to manage it.
- **Target Membership Issue**: Inspection of `CBTReframe.xcodeproj/project.pbxproj` reveals that `ExerciseLiveActivity.swift` is only included in the `ExerciseWidgetExtension` target. This means the main app (`CBTReframe`) has no visibility into `ExerciseAttributes` and therefore cannot request a Live Activity.
- **Info.plist**: `CBTReframe/Info.plist` is missing the required `<key>NSSupportsLiveActivities</key>` set to `<true/>` for ActivityKit to function properly.

## 2. Logic Chain
- **Memory Leak Fix**: The `Task` must not capture `self` strongly for the duration of the suspension. By using optional chaining (`try await self?.viewModel.state.collect(collector: collector)`), `self` is only captured strongly to evaluate the left-hand side (`viewModel.state`). Once the reference is resolved and `collect` begins, the strong reference is released, allowing `ObservableExerciseSession` to be safely deallocated.
- **Shared Architecture for ActivityKit**: The Live Activity cannot be requested by the main app if it does not know the attribute structure. Therefore, `ExerciseAttributes` must be extracted from `ExerciseLiveActivity.swift` into a separate file (`ExerciseAttributes.swift`) and added to BOTH the `CBTReframe` and `ExerciseWidgetExtension` targets.
- **Live Activity Management**: `ObservableExerciseSession` in `ExerciseSessionView.swift` observes state changes from the ViewModel. It is the logical owner of the Live Activity lifecycle. It must call `Activity.request` on start, `Activity.update` inside the state collector when state changes, and `Activity.end` when the session is stopped.
- **Info.plist**: Adding the `NSSupportsLiveActivities` key ensures the system grants permission to the app to display Live Activities.

## 3. Caveats
- The app's deployment target is currently at least iOS 16.1. ActivityKit is fully supported in this environment without needing `#available(iOS 16.1, *)` wrappers.

## 4. Conclusion & Implementation Plan
Implement the following comprehensive strategy:

1. **Info.plist configuration**:
   - Add `<key>NSSupportsLiveActivities</key>` and `<true/>` to `CBTReframe/Info.plist`.

2. **Extract `ExerciseAttributes`**:
   - Create a new file `CBTReframe/LiveActivity/ExerciseAttributes.swift` with `ActivityKit` imported and containing the `ExerciseAttributes` struct.
   - Remove the `ExerciseAttributes` struct from `ExerciseLiveActivity.swift`.
   - Write a Ruby script using the `xcodeproj` gem to add `ExerciseAttributes.swift` to both `CBTReframe` and `ExerciseWidgetExtension` targets. (e.g., using `main_target.add_file_references` and `widget_target.add_file_references`).

3. **Fix Memory Leak & Add ActivityKit Logic in `ExerciseSessionView.swift`**:
   - Add `import ActivityKit`.
   - Update `ObservableExerciseSession` to hold a reference to `definition: ExerciseDefinition` and `liveActivity: Activity<ExerciseAttributes>?`.
   - Update `init` to: `init(definition: ExerciseDefinition) { self.definition = definition; self.viewModel = ExerciseSessionViewModel(definition: definition); ... }`.
   - Update `ExerciseSessionView` initializer to: `_session = StateObject(wrappedValue: ObservableExerciseSession(definition: definition))`.
   - Modify `startObserving()` to remove `guard let self = self` and use `try await self?.viewModel.state.collect(collector: collector)`.
   - Add `startLiveActivity()` inside `startObserving()`. It should check `ActivityAuthorizationInfo().areActivitiesEnabled` and call `Activity.request`.
   - Add `updateLiveActivity(with newState: ExerciseSessionState)` and call it within the `collector`'s `DispatchQueue.main.async` block using `Activity.update`.
   - Add `endLiveActivity()` inside `stopObserving()` to gracefully call `Activity.end`.

## 5. Verification Method
- **Compiler**: Run `xcodebuild -project CBTReframe.xcodeproj -scheme CBTReframe -sdk iphonesimulator build` to verify the code compiles without errors.
- **Integrity**: Check `project.pbxproj` manually or via script to confirm `ExerciseAttributes.swift` belongs to both targets.
- **Code Review**: Ensure `Activity.request`, `update`, and `end` exist in `ExerciseSessionView.swift`.
