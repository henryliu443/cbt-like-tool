# Phase 1: Explorer Handoff Report

## Observation
1. **Memory Leak**: In `CBTReframe/Views/Exercises/ExerciseSessionView.swift`, the `startObserving()` method contains a `Task` with `guard let self = self else { return }`. Because `self.viewModel.state.collect(collector:)` runs indefinitely, `self` is held strongly indefinitely, preventing `deinit` from running and causing a memory leak (Critic VETO).
2. **Missing Live Activity Management**: `CBTReframe/LiveActivity/ExerciseLiveActivity.swift` contains both `ExerciseAttributes` and the widget entry point `@main struct ExerciseLiveActivity`.
3. **Target Visibility Issue**: Searching `project.pbxproj` reveals that `ExerciseLiveActivity.swift` is only included in the `ExerciseWidgetExtension` target. The main app target (`CBTReframe`) cannot access `ExerciseAttributes` to request, update, or end the activity. Attempting to add `ExerciseLiveActivity.swift` to the main app target would cause a duplicate `@main` error.
4. **State Updates Throttle**: The `ExerciseSessionViewModel` ticks every 50ms, meaning `collect` emits new state 20 times per second. `ActivityKit` limits update frequency, requiring throttled updates.

## Logic Chain
1. To fix the memory leak, we must use optional chaining `self?.` inside the `collectionTask` instead of `guard let self = self`, ensuring the closure doesn't strongly retain `self` across the suspension point of the infinite flow collector.
2. To integrate ActivityKit properly, the main app (`ExerciseSessionView.swift`) must call `Activity.request`, `update`, and `end`.
3. To share `ExerciseAttributes` with the main app without `@main` conflicts, it must be extracted into a new file `ExerciseAttributes.swift` and added to BOTH `CBTReframe` and `ExerciseWidgetExtension` targets in `project.pbxproj`.
4. `ObservableExerciseSession` in `ExerciseSessionView.swift` should manage the Live Activity instance (`Activity<ExerciseAttributes>`). It needs access to the total cycles and exercise name, so `ExerciseDefinition` must be passed into its initializer.
5. Live Activity updates should be triggered from the `collector` block in `startObserving()`, but only when meaningful data (`remainingTime`, `phase`, `cycle`) changes, ignoring the 50ms sub-second progress updates to comply with ActivityKit update limits.

## Caveats
- Modifying `project.pbxproj` manually via scripts can be error-prone; the implementer needs to carefully add the new file reference and build file entries to both targets, or use a tool like `xcodeproj`.
- `Activity.end` is asynchronous and requires `Task { await activity.end(...) }`. In `deinit` it's best-effort since the object is being deallocated, but `clear()` is called consistently on `onDisappear` which serves as the primary cleanup.

## Conclusion
The memory leak is isolated to the strong capture in `startObserving()`. The ActivityKit omission requires structural changes:
1. Extract `ExerciseAttributes` to `CBTReframe/LiveActivity/ExerciseAttributes.swift` and add it to both targets in `project.pbxproj`.
2. Update `ObservableExerciseSession` in `ExerciseSessionView.swift` to manage `liveActivity: Activity<ExerciseAttributes>?` with methods `startLiveActivity()`, `updateLiveActivity()`, and `endLiveActivity()`.
3. Bind `startLiveActivity()` to `start()`, `endLiveActivity()` to `clear()` and `deinit`, and selectively call `updateLiveActivity()` inside the `collector` only when seconds, phase, or cycle differ from the previous state.

## Verification Method
1. **Memory Leak**: Open the app, start an exercise, exit the view. Print logs or breakpoints should confirm `deinit` in `ObservableExerciseSession` is called.
2. **Target Isolation**: Build the `CBTReframe` target. It should compile without `Cannot find type 'ExerciseAttributes'` errors.
3. **Live Activity**: Start an exercise, minimize the app. The Live Activity should appear on the lock screen and Dynamic Island. Pause, Reset, and wait for phase transitions; the Live Activity should update accurately. Exiting the view should dismiss the Live Activity.

## Proposed Code Changes

### 1. `CBTReframe/LiveActivity/ExerciseAttributes.swift` (New File)
```swift
import ActivityKit
import shared

public struct ExerciseAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        public var phaseDisplayName: String
        public var remainingTime: Int
        public var cycle: Int
        public var totalCycles: Int
        
        public init(phaseDisplayName: String, remainingTime: Int, cycle: Int, totalCycles: Int) {
            self.phaseDisplayName = phaseDisplayName
            self.remainingTime = remainingTime
            self.cycle = cycle
            self.totalCycles = totalCycles
        }
    }
    
    public var exerciseName: String
    
    public init(exerciseName: String) {
        self.exerciseName = exerciseName
    }
}
```
*(Remember to remove this struct from `ExerciseLiveActivity.swift` and add this new file to BOTH targets in `project.pbxproj`)*

### 2. `CBTReframe/Views/Exercises/ExerciseSessionView.swift`
```swift
import SwiftUI
import ActivityKit
import shared

class ObservableExerciseSession: ObservableObject {
    let viewModel: ExerciseSessionViewModel
    let definition: ExerciseDefinition
    @Published var state: ExerciseSessionState
    
    private var collectionTask: Task<Void, Never>?
    private var liveActivity: Activity<ExerciseAttributes>?
    
    init(viewModel: ExerciseSessionViewModel, definition: ExerciseDefinition) {
        self.viewModel = viewModel
        self.definition = definition
        self.state = viewModel.state.value as! ExerciseSessionState
    }
    
    func startObserving() {
        stopObserving()
        
        let collector = FlowCollector<ExerciseSessionState> { [weak self] newState in
            DispatchQueue.main.async {
                guard let self = self else { return }
                
                let oldRemaining = self.state.remainingTime
                let oldPhase = self.state.phase.name
                let oldCycle = self.state.cycle
                
                self.state = newState
                
                // Throttle updates for Live Activity
                if oldRemaining != newState.remainingTime || 
                   oldPhase != newState.phase.name || 
                   oldCycle != newState.cycle {
                    self.updateLiveActivity()
                }
            }
        }
        
        collectionTask = Task { [weak self] in
            // FIX: Removed `guard let self = self` to prevent memory leak
            do {
                try await self?.viewModel.state.collect(collector: collector)
            } catch {
                if !(error is CancellationError) {
                    print("StateFlow collection error: \(error)")
                }
            }
        }
    }
    
    func stopObserving() {
        collectionTask?.cancel()
        collectionTask = nil
    }
    
    private func startLiveActivity() {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }
        
        let attributes = ExerciseAttributes(exerciseName: definition.name)
        let contentState = ExerciseAttributes.ContentState(
            phaseDisplayName: state.phase.displayName,
            remainingTime: Int(state.remainingTime),
            cycle: Int(state.cycle),
            totalCycles: Int(definition.totalCycles)
        )
        let content = ActivityContent(state: contentState, staleDate: nil)
        
        do {
            liveActivity = try Activity.request(attributes: attributes, content: content)
        } catch {
            print("Failed to start Live Activity: \(error)")
        }
    }
    
    private func updateLiveActivity() {
        guard let activity = liveActivity else { return }
        
        let contentState = ExerciseAttributes.ContentState(
            phaseDisplayName: state.phase.displayName,
            remainingTime: Int(state.remainingTime),
            cycle: Int(state.cycle),
            totalCycles: Int(definition.totalCycles)
        )
        let content = ActivityContent(state: contentState, staleDate: nil)
        
        Task {
            await activity.update(content)
        }
    }
    
    private func endLiveActivity() {
        guard let activity = liveActivity else { return }
        
        let contentState = ExerciseAttributes.ContentState(
            phaseDisplayName: state.phase.displayName,
            remainingTime: Int(state.remainingTime),
            cycle: Int(state.cycle),
            totalCycles: Int(definition.totalCycles)
        )
        let content = ActivityContent(state: contentState, staleDate: nil)
        
        Task {
            await activity.end(content, dismissalPolicy: .immediate)
        }
        liveActivity = nil
    }
    
    func start() { 
        viewModel.start() 
        if liveActivity == nil {
            startLiveActivity()
        }
    }
    func pause() { viewModel.pause() }
    func reset() { 
        viewModel.reset() 
        updateLiveActivity()
    }
    func clear() { 
        stopObserving()
        viewModel.clear() 
        endLiveActivity()
    }
    
    deinit {
        stopObserving()
        if let activity = liveActivity {
            Task {
                await activity.end(nil, dismissalPolicy: .immediate)
            }
        }
    }
}

// ... FlowCollector remains unchanged ...

struct ExerciseSessionView: View {
    @StateObject private var session: ObservableExerciseSession
    
    init(definition: ExerciseDefinition) {
        // FIX: Pass definition
        _session = StateObject(wrappedValue: ObservableExerciseSession(viewModel: ExerciseSessionViewModel(definition: definition), definition: definition))
    }
    
    // ... body remains unchanged ...
}
```
