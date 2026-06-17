# Handoff Report: Memory Leaks in Exercise Session and Haptics

## Observation
1. In `CBTReframe/Views/Exercises/ExerciseSessionView.swift` (line 24), `ObservableExerciseSession.startObserving()` creates a `Task` and accesses `viewModel.state` within it without an explicit capture list. This implicitly captures `self` strongly. The `Task` is stored in `self.collectionTask`.
2. In `CBTReframe/Views/Exercises/ExerciseSessionView.swift` (line 17), `startObserving()` directly assigns a new `Task` to `collectionTask` without calling `.cancel()` on the existing task first.
3. In `CBTReframe/Haptic/AdvancedHapticEngine.swift` (line 36), `AdvancedHapticEngine.startContinuousHaptic()` creates and starts a new `CHHapticAdvancedPatternPlayer` (stored in `continuousPlayer`) without stopping the previous player if one was already running. The event has `duration: .greatestFiniteMagnitude`.
4. In `CBTReframe/Views/Exercises/ExerciseSessionView.swift`, the `ObservableExerciseSession` class does not implement a `deinit` block to cancel the `collectionTask`.

## Logic Chain
1. **Retain Cycle:** Because `self` holds a strong reference to `collectionTask`, and the `Task` closure strongly captures `self` (to access `self.viewModel`), a retain cycle is formed (`self` -> `collectionTask` -> `Task` -> `self`). This prevents `ObservableExerciseSession` from ever being deallocated. To fix this, `[weak self]` must be explicitly declared in the `Task` closure capture list.
2. **Compounding Tasks/Players:** If `.onAppear` is triggered multiple times in SwiftUI (e.g., navigating away and back, or during certain view updates), `startObserving()` and `startContinuousHaptic()` are called again. Without prior cleanup, the previous Kotlin state flow collection task continues running in the background. Similarly, the previous haptic player continues vibrating indefinitely (due to its infinite duration), while a new one is layered on top. To fix this, `startObserving()` must call `stopObserving()` (or cancel the existing task) first, and `startContinuousHaptic()` must call `stopContinuousHaptic()` before creating a new player.
3. **Deallocation Safety:** Even if the retain cycle is broken, if the view is dismissed and `stopObserving()` isn't explicitly called (e.g. if `onDisappear` fails to execute reliably), the `Task` will outlive the session. A `deinit` block in `ObservableExerciseSession` that explicitly cancels `collectionTask` guarantees that background work stops when the session object is deallocated.

## Caveats
- The investigation relies on the provided Swift code and static analysis of memory management patterns in Swift. I did not run the app in Instruments to profile memory.
- There may be other memory issues in the app, but this directly addresses the three issues flagged by the VETO feedback.
- `ExerciseSessionView` uses `@StateObject`, which manages the lifecycle of `ObservableExerciseSession` based on the View's lifecycle. `onDisappear` is meant to call `session.clear()`, but view lifecycle events in SwiftUI can sometimes be unpredictable, emphasizing the need for robust `deinit` and duplicate-call protections.

## Conclusion
To fix the VETO feedback regarding memory leaks, the following strategy should be implemented:
1. **Fix Retain Cycle:** In `ObservableExerciseSession.startObserving()`, add `[weak self]` to the `Task` capture list to avoid strongly capturing `self`.
2. **Prevent Duplicate Tasks/Players:**
   - In `ObservableExerciseSession.startObserving()`, call `stopObserving()` before creating a new task.
   - In `AdvancedHapticEngine.startContinuousHaptic()`, call `stopContinuousHaptic()` before creating and starting a new `continuousPlayer`.
3. **Guarantee Cancellation:** Add a `deinit` block to `ObservableExerciseSession` that explicitly calls `collectionTask?.cancel()`.

## Verification Method
1. **Implementation Check:** Inspect `ExerciseSessionView.swift` and `AdvancedHapticEngine.swift` after changes to confirm `[weak self]` in the task, explicit cancellations at the start of the `start*` functions, and the presence of `deinit`.
2. **Runtime Verification:** Run the iOS app. Navigate to the exercise session view, leave, and re-enter multiple times. Confirm that device vibration does not compound or run out of control.
3. **Memory Profiling:** Use Xcode Instruments (Leaks / Allocations) to verify that `ObservableExerciseSession` instances are properly deallocated after leaving the view and that no lingering `Task` closures remain in memory.
