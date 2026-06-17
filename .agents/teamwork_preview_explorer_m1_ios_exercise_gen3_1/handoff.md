# Handoff: iOS Memory Leaks Fix Strategy

## Observation

1. **Retain Cycle in `ExerciseSessionView.swift`**:
   In `ObservableExerciseSession.startObserving()` (lines 24-32), the `Task` used to collect the `StateFlow` implicitly captures `self` strongly by accessing `viewModel`:
   ```swift
   collectionTask = Task {
       do {
           try await viewModel.state.collect(collector: collector)
       ...
   }
   ```
2. **Duplicate Task/Player Initialization**:
   - In `ExerciseSessionView.swift` (lines 17-33), `startObserving()` does not cancel `collectionTask` before creating a new one. It overwrites the reference, leaking the previous running task.
   - In `AdvancedHapticEngine.swift` (lines 36-49), `startContinuousHaptic()` creates and starts a new `CHHapticAdvancedPatternPlayer` without stopping or cleaning up the previous `continuousPlayer`.
3. **Missing `deinit`**:
   `ObservableExerciseSession` in `ExerciseSessionView.swift` does not implement a `deinit` block.

## Logic Chain

1. **Addressing Retain Cycle**: The implicit strong capture of `self` prevents `ObservableExerciseSession` from ever being deallocated. This `Task` should explicitly capture `[weak self]` so it does not hold a strong reference back to the session object.
2. **Addressing Compounding Resources**: In SwiftUI, `.onAppear` may be invoked multiple times (e.g., due to view hierarchy changes or navigation) without `.onDisappear` being guaranteed to fire. Consequently, multiple duplicate flow collection tasks and haptic pattern players can be spawned. Prepending cleanup calls (e.g., `stopObserving()` and `stopContinuousHaptic()`) at the start of these start functions ensures idempotency.
3. **Addressing Lifecycle Safety**: Even with `[weak self]`, the `Task` can still run in the background if the view is destroyed but not explicitly cleaned up. Adding a `deinit` block that cancels the `collectionTask` guarantees that observation halts the moment the object is reclaimed.

## Caveats

- We are assuming `stopContinuousHaptic()` is safe to call when a player hasn't been instantiated yet, which it is due to optional chaining (`continuousPlayer?.stop(...)`).
- Cancelling `Task` relies on Kotlin's coroutines appropriately reacting to Swift task cancellation. `viewModel.state.collect` generally bridges cancellation appropriately via KMP, throwing a `CancellationError` on the Swift side.

## Conclusion

**Fix Strategy Recommendations:**

1. **Fix `ObservableExerciseSession` Task Retain Cycle**:
   Update `startObserving()` in `ExerciseSessionView.swift` to capture `[weak self]` in the task:
   ```swift
   collectionTask = Task { [weak self] in
       guard let self = self else { return }
       do {
           try await self.viewModel.state.collect(collector: collector)
       ...
   ```
2. **Prevent Duplicate Initializations**:
   - Add `stopObserving()` at the very beginning of `startObserving()` in `ExerciseSessionView.swift`.
   - Add `stopContinuousHaptic()` at the very beginning of `startContinuousHaptic()` in `AdvancedHapticEngine.swift`.
3. **Implement `deinit` for `ObservableExerciseSession`**:
   Add the following to `ObservableExerciseSession` in `ExerciseSessionView.swift`:
   ```swift
   deinit {
       stopObserving()
   }
   ```

## Verification Method
1. Make the recommended changes to `ExerciseSessionView.swift` and `AdvancedHapticEngine.swift`.
2. Build the project using `xcodebuild` or from Xcode to ensure the Swift code is valid.
3. Run the iOS App, navigate to the Exercise feature, and toggle away and back multiple times. Ensure device doesn't have runaway haptic feedback and Xcode memory graph doesn't show multiple `ObservableExerciseSession` instances or leaked `Task` objects.
