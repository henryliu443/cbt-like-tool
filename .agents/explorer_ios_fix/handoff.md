# Handoff Report: iOS Native Exercise Interface Bug Fixes

## 1. Observation

**Memory Leak in `ExerciseSessionView.swift`:**
- In `CBTReframe/Views/Exercises/ExerciseSessionView.swift:26-35`, the `startObserving()` method creates a Task:
  ```swift
          collectionTask = Task { [weak self] in
              guard let self = self else { return }
              do {
                  try await self.viewModel.state.collect(collector: collector)
              // ...
  ```
- The `guard let self = self` turns the weak reference into a strong reference.

**Live Activity Integration Missing:**
- The Live Activity is defined in `CBTReframe/LiveActivity/ExerciseLiveActivity.swift` which includes `ExerciseAttributes` and the Widget UI.
- Searching the `CBTReframe.xcodeproj/project.pbxproj` reveals that `ExerciseLiveActivity.swift` is only included in the `ExerciseWidgetExtension` native target (source phase `48B56F213152E8BDC7576106`).
- Searching the codebase reveals no calls to ActivityKit's `Activity.request`, `update`, or `end`.
- `CBTReframe/Info.plist` does not contain the `NSSupportsLiveActivities` key.
- `ExerciseDefinition` (in KMP shared code) contains `totalCycles: Int`, which is required for the ActivityKit content state, but is currently not exposed in `ExerciseSessionView`.

## 2. Logic Chain

1. **Memory Leak Fix:**
   - Because `collect(collector:)` on a Kotlin `StateFlow` suspends indefinitely awaiting new emissions, the Task remains alive until explicitly cancelled.
   - The strong `self` reference means `ObservableExerciseSession` is retained by the Task. This creates a retain cycle: the session holds the `collectionTask`, and the Task holds the session. `deinit` is never called, so the Task is never cancelled.
   - Removing `guard let self = self` and using optional chaining (`try await self?.viewModel.state.collect...`) allows the Task to execute without preventing `deinit`. When the View disappears and the `ObservableExerciseSession` is deallocated, `deinit` will be called, cancelling the Task.

2. **Live Activity Architecture:**
   - For the main app to start a Live Activity, it must pass `ExerciseAttributes` to `Activity.request`. However, `ExerciseAttributes` is currently inside `ExerciseLiveActivity.swift`, which is only compiled for the widget extension. We must separate it into a new file `ExerciseAttributes.swift` and compile it for both targets.
   - ActivityKit requires `NSSupportsLiveActivities` in `Info.plist` to be `YES` to function.
   - A dedicated `LiveActivityManager` in the main app should wrap the `Activity<ExerciseAttributes>` logic to keep the view clean.
   - The View or `ObservableExerciseSession` needs to trigger these ActivityKit updates. We can hook into `.onAppear` (to request), `.onDisappear` (to end), and `.onChange(of: session.state.remainingTime)` or the Flow collector (to update).
   - To construct `ExerciseAttributes.ContentState`, we need `totalCycles`, which can be injected into `ObservableExerciseSession` from the `ExerciseDefinition` available during init.

## 3. Caveats
- I did not verify if the ActivityKit update frequency (updating every 1 second when `remainingTime` changes) exceeds iOS limits, but typically 1/second is acceptable for active workout/exercise Live Activities.
- Adding files to the `.pbxproj` must be done carefully to ensure the file references and build phases are correctly formatted, preferably using a Ruby script in the implementer phase.

## 4. Conclusion

**Actionable Implementation Plan:**

1. **Fix Memory Leak:**
   - In `ExerciseSessionView.swift`, modify `startObserving()`:
     ```swift
     collectionTask = Task { [weak self] in
         do {
             try await self?.viewModel.state.collect(collector: collector)
         } catch { ... }
     }
     ```

2. **Refactor Live Activity Attributes:**
   - Extract `ExerciseAttributes` and its `ContentState` from `ExerciseLiveActivity.swift` into a new file `CBTReframe/LiveActivity/ExerciseAttributes.swift`.
   - Add `ExerciseAttributes.swift` to both `CBTReframe` and `ExerciseWidgetExtension` targets in `project.pbxproj`.

3. **Enable Live Activities:**
   - Add `<key>NSSupportsLiveActivities</key><true/>` to `CBTReframe/Info.plist`.

4. **Implement LiveActivityManager:**
   - Create `CBTReframe/LiveActivity/LiveActivityManager.swift` (added to `CBTReframe` target only) wrapping `Activity.request`, `Activity.update`, and `Activity.end`.

5. **Integrate with View/Session:**
   - Update `ObservableExerciseSession` to hold a reference to `ExerciseDefinition` (e.g. `let definition: ExerciseDefinition`) passed via `init`.
   - In `ExerciseSessionView.swift`, call `LiveActivityManager.shared.start` on appear, `.end()` on disappear, and `.update()` when `session.state.remainingTime` changes, passing `totalCycles: Int(session.definition.totalCycles)`.

## 5. Verification Method
- **Memory Leak:** Run the app, navigate to the Exercise session view, and exit. Ensure `deinit` inside `ObservableExerciseSession` is called (can be verified with a `print` statement).
- **Live Activity:** Build and run the app on a simulator or device (iOS 16.1+). Start an exercise. Lock the screen or swipe to the home screen; verify the Live Activity / Dynamic Island appears and updates the remaining time correctly. When the exercise is exited, the Live Activity should dismiss.
- **Compilation:** Run `xcodebuild -project CBTReframe.xcodeproj -scheme CBTReframe -destination 'platform=iOS Simulator,name=iPhone 15'` to ensure both targets compile successfully after the file movements.
