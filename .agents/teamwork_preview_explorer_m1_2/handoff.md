# M1 KMP State Machine Analysis Report

## 1. Observation
- The target package directory `shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/` exists and is meant for KMP Shared Session Module state machine logic according to `SCOPE.md`.
- Inspecting this directory using `list_dir` reveals that exactly one file currently exists: `ExercisePhase.kt`. 
- `ExercisePhase.kt` currently defines `INHALE`, `HOLD_IN`, `EXHALE`, `HOLD_OUT`, `REST`. However, `SCOPE.md` asks for an enum with `Inhale`, `Hold`, `Exhale`, `Rest`.
- There are no existing implementations for `ExerciseDefinition.kt`, `ExerciseSessionState.kt`, or `ExerciseSessionViewModel.kt` in the project.

## 2. Logic Chain
- To fulfill the M1 KMP State Machine requirements without scattering code, all state machine files should be co-located in the designated `session` package.
- `ExercisePhase.kt` already exists but needs its enum values aligned with the scope requirements (`Inhale, Hold, Exhale, Rest`).
- `ExerciseDefinition.kt` needs to be created to represent a configurable sequence of phases (e.g., `PhaseSequence` or a list of phase-duration pairs).
- `ExerciseSessionState.kt` needs to be a data class holding: `phase: ExercisePhase`, `progress: Float`, `phaseDuration: Long`, `remainingTime: Long`, `totalCycleProgress: Float`, `isPaused: Boolean`, and `cycle: Int`.
- `ExerciseSessionViewModel.kt` needs to manage the timer logic. It should maintain a `MutableStateFlow<ExerciseSessionState>`, launch a coroutine when unpaused that runs a while loop (e.g. `while(isActive && !isPaused)`). It should use `delay(16L)` or a similar frame-rate delay to deduct time from `remainingTime`, calculate `progress` (based on `phaseDuration` - `remainingTime`), transition to the next phase when `remainingTime <= 0`, update `totalCycleProgress`, and increment `cycle` upon completing a full sequence.

## 3. Caveats
- Since the iOS implementation will consume these states, it's critical that the `ExerciseSessionViewModel` handles coroutine lifecycle properly and exposes an objective-C friendly `StateFlow` if necessary (though KMP handles basic StateFlow well).
- The existing `ExercisePhase.kt` has `HOLD_IN` and `HOLD_OUT`. Replacing them with a single `Hold` could affect other parts of the codebase if they currently use the old values, but since no other files use this logic yet (as seen from grep), modifying the enum is safe.

## 4. Conclusion
The four requested state machine files should be created/updated at the following exact paths:
1. `/Users/henry/cbt-like-tool/shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/ExercisePhase.kt` (Update existing)
2. `/Users/henry/cbt-like-tool/shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/ExerciseDefinition.kt` (Create new)
3. `/Users/henry/cbt-like-tool/shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/ExerciseSessionState.kt` (Create new)
4. `/Users/henry/cbt-like-tool/shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/ExerciseSessionViewModel.kt` (Create new)

**Implementation Strategy:**
- Define `ExercisePhase` with `INHALE, HOLD, EXHALE, REST`.
- Define `ExerciseDefinition` as a data class holding a list of phase configurations (`val phases: List<PhaseDuration>`).
- Implement `ExerciseSessionState` as a straightforward data class with the properties outlined in the scope.
- In `ExerciseSessionViewModel`, use `viewModelScope` (or a custom CoroutineScope for KMP) to start a timer that deducts time every frame tick (`delay(16L)`), updating the `MutableStateFlow` with accurate remaining time and recalculating progress floats. When `remainingTime` reaches 0, advance to the next index in the `ExerciseDefinition` sequence.

## 5. Verification Method
- **Implementation:** Check that the 4 files exist at the targeted paths and contain the correct declarations.
- **Compilation:** Run `./gradlew :shared:build` to ensure the KMP shared code compiles successfully without errors.
- **Unit Tests:** Run tests for the `ExerciseSessionViewModel` to verify that phases transition correctly over time and states are properly emitted to the `StateFlow`.
