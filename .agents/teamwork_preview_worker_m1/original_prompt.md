## 2026-06-14T05:10:30Z
You are a Worker for the M1 KMP State Machine milestone.
Your task is to implement the changes recommended by the Explorers.

Working Directory: /Users/henry/cbt-like-tool/.agents/teamwork_preview_worker_m1

Implementation Strategy:
- Target Directory: /Users/henry/cbt-like-tool/shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/
- Update ExercisePhase.kt to have: `Inhale`, `Hold`, `Exhale`, `Rest`.
- Create ExerciseDefinition.kt (configurable PhaseSequence).
- Create ExerciseSessionState.kt (data class with phase, progress, phaseDuration, remainingTime, totalCycleProgress, isPaused, cycle).
- Create ExerciseSessionViewModel.kt (Coroutine-driven timer emitting StateFlow<ExerciseSessionState>).
- Write or run tests to verify compilation (`./gradlew :shared:build`).

When you are done, document your changes in a handoff.md report and run the build command. Return the path to the report.

DO NOT CHEAT. All implementations must be genuine. DO NOT
hardcode test results, create dummy/facade implementations, or
circumvent the intended task. A Forensic Auditor will independently
verify your work. Integrity violations WILL be detected and your
work WILL be rejected.

## 2026-06-14T05:10:34Z
One more finding from an Explorer:
Based on other KMP ViewModels in the project, `ExerciseSessionViewModel` should be a standard Kotlin class with a `CoroutineScope` and a `clear()` method, rather than inheriting from Android's `ViewModel`. Please ensure you follow this architecture when implementing the ViewModel.
