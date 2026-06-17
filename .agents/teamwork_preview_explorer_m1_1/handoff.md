# Handoff Report: M1 KMP State Machine

## 1. Observation
- The target module for the state machine is `shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/`.
- `ExercisePhase.kt` already exists at `/Users/henry/cbt-like-tool/shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/ExercisePhase.kt` and contains the `INHALE`, `HOLD_IN`, `EXHALE`, `HOLD_OUT`, `REST` enum values.
- `ExerciseDefinition.kt`, `ExerciseSessionState.kt`, and `ExerciseSessionViewModel.kt` do not currently exist in the codebase.

## 2. Logic Chain
- Since `ExercisePhase.kt` already exists with the required enumerations, we do not need to create it, but we will reference it.
- Based on `SCOPE.md`, the state machine files belong in the KMP Shared Session Module. Therefore, the new files should be created in the `com.henryliu.cbtreframe.shared.session` package.
- `ExerciseDefinition.kt` should define a sequence of phases and their durations, which configures the view model.
- `ExerciseSessionState.kt` should be a pure Kotlin `data class` with exactly the properties requested.
- `ExerciseSessionViewModel.kt` should encapsulate a coroutine `tick` loop (running at ~16-50ms intervals) that recalculates progress and transitions between phases/cycles based on `ExerciseDefinition`, updating a `MutableStateFlow<ExerciseSessionState>`.

## 3. Caveats
- `ExerciseSessionViewModel.kt` is technically a ViewModel. While other view models (like `HistoryViewModel`) are placed in `viewmodels/`, the scope explicitly states `shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/` for the state machine logic. It is safer to group them in `session/`.
- The tick interval for the Coroutine timer is not specified. A `delay(16L)` or `delay(50L)` is recommended for smooth UI progress rendering.

## 4. Conclusion
Create the following files with the specified concrete implementation strategy:

**1. `/Users/henry/cbt-like-tool/shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/ExerciseDefinition.kt`**
```kotlin
package com.henryliu.cbtreframe.shared.session

data class PhaseDuration(val phase: ExercisePhase, val durationMs: Long)

data class ExerciseDefinition(
    val name: String,
    val phases: List<PhaseDuration>,
    val totalCycles: Int
)
```

**2. `/Users/henry/cbt-like-tool/shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/ExerciseSessionState.kt`**
```kotlin
package com.henryliu.cbtreframe.shared.session

data class ExerciseSessionState(
    val phase: ExercisePhase = ExercisePhase.REST,
    val progress: Float = 0f,
    val phaseDuration: Long = 0L,
    val remainingTime: Long = 0L,
    val totalCycleProgress: Float = 0f,
    val isPaused: Boolean = false,
    val cycle: Int = 1
)
```

**3. `/Users/henry/cbt-like-tool/shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/ExerciseSessionViewModel.kt`**
```kotlin
package com.henryliu.cbtreframe.shared.session

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExerciseSessionViewModel(
    private val definition: ExerciseDefinition,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(ExerciseSessionState())
    val uiState: StateFlow<ExerciseSessionState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    // Concrete strategy:
    // 1. `start()` launches a coroutine with a `while(isActive)` loop.
    // 2. `delay(50)` on each tick.
    // 3. Accumulate elapsed time, calculate current phase based on total time elapsed or phase-specific elapsed time.
    // 4. Update `_uiState.value` with new progress, time, and phase.
    // 5. Provide `pause()` and `resume()` to toggle `isPaused` state.
}
```

## 5. Verification Method
- After implementation, verify that all three files compile successfully in the `commonMain` target.
- Write unit tests for `ExerciseSessionViewModel.kt` in `shared/src/commonTest` to verify that `uiState.value.phase` transitions correctly after the specified `durationMs` has elapsed.
