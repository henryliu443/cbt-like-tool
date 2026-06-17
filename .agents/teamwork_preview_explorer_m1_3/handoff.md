# Analysis Report

## 1. Observation
1. The target directory `shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/` exists and already contains an `ExercisePhase.kt` file.
2. The existing `ExercisePhase.kt` defines five states: `INHALE`, `HOLD_IN`, `EXHALE`, `HOLD_OUT`, `REST`. 
3. Examining existing viewmodels (e.g., `MoodInsightsViewModel.kt`), KMP viewmodels in this project are standard Kotlin classes that do not inherit from a platform `ViewModel` class. They receive a `CoroutineScope` (defaulting to `CoroutineScope(Dispatchers.Main + SupervisorJob())`), manage their own `MutableStateFlow` converted to a `StateFlow` via `.stateIn` or `.asStateFlow()`, and have a `clear()` method to cancel the scope.

## 2. Logic Chain
1. **Target Paths**: All required components fit the stated scope of the "KMP Shared Session Module" and should reside exactly in `shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/`.
2. **ExercisePhase**: The requirement specifically asks for `Inhale`, `Hold`, `Exhale`, `Rest`. The existing file should be refactored to consolidate `HOLD_IN` and `HOLD_OUT` into a single `HOLD` state to meet the requirement.
3. **ExerciseDefinition**: Needs to represent a "configurable PhaseSequence". This should be modeled as a data class holding a list of phase-duration pairs, which the viewmodel can iterate through sequentially for a single cycle.
4. **ExerciseSessionState**: Needs to be a data class holding exactly the properties requested (`phase`, `progress`, `phaseDuration`, `remainingTime`, `totalCycleProgress`, `isPaused`, `cycle`).
5. **ExerciseSessionViewModel**: Following project conventions, this should be a standard class holding a `MutableStateFlow<ExerciseSessionState>` and exposing it as a `StateFlow<ExerciseSessionState>`. A coroutine launched within the provided `CoroutineScope` will serve as the timer (using `while(isActive && !isPaused) { delay(tickRate); updateState() }`) to drive progress and cycle through the `ExerciseDefinition` phases.

## 3. Caveats
- Condensing `HOLD_IN` and `HOLD_OUT` into a single `HOLD` phase means the UI won't distinguish between holding breath after an inhale vs holding after an exhale purely from the enum, but it satisfies the explicit requirement.
- The `ExerciseSessionViewModel` timer tick rate (e.g., `delay(16)` for 60fps or `delay(50)` for ~20fps) will dictate UI smoothness and should be decided by the implementer.

## 4. Conclusion
The exact target paths and implementation strategy are as follows:

- **Modify**: `shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/ExercisePhase.kt`
  - Update the enum to: `enum class ExercisePhase { INHALE, HOLD, EXHALE, REST }` (optionally keeping display names/active flags if still needed).

- **Create**: `shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/ExerciseDefinition.kt`
  - Implement a configurable phase sequence structure, e.g., `data class ExerciseDefinition(val name: String, val phases: List<PhaseDuration>)` where `PhaseDuration` defines an `ExercisePhase` and its length in milliseconds.

- **Create**: `shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/ExerciseSessionState.kt`
  - Implement as: `data class ExerciseSessionState(val phase: ExercisePhase, val progress: Float, val phaseDuration: Long, val remainingTime: Long, val totalCycleProgress: Float, val isPaused: Boolean, val cycle: Int)` with default values.

- **Create**: `shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/ExerciseSessionViewModel.kt`
  - Implement as a standard class taking `private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())`.
  - Expose `val state: StateFlow<ExerciseSessionState>`.
  - Implement the timer loop via a suspended coroutine function that calculates elapsed time vs `phaseDuration` to update `progress` (0f to 1f) and transitions phases when `remainingTime` hits 0.
  - Implement standard actions: `play()`, `pause()`, and `clear()`.

## 5. Verification Method
- After implementation, verify that all files exist in `shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/`.
- Run `./gradlew :shared:build` or `./gradlew :shared:compileKotlinIosSimulatorArm64` to ensure KMP shared logic compiles without platform-specific errors.
- Confirm `ExerciseSessionViewModel` uses `kotlinx.coroutines` and standard Kotlin flows without Android dependencies.
