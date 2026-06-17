## 2026-06-14T13:09:00Z
You are an Explorer for the M1 KMP State Machine milestone.
Your task is to analyze the codebase and recommend a concrete fix/implementation strategy. Do not implement the code yourself.

Scope Document: /Users/henry/cbt-like-tool/.agents/sub_orch_m1/SCOPE.md
Working Directory: /Users/henry/cbt-like-tool/.agents/teamwork_preview_explorer_m1_3

Requirements:
- Create ExercisePhase.kt (enum for Inhale, Hold, Exhale, Rest).
- Create ExerciseDefinition.kt (configurable PhaseSequence).
- Create ExerciseSessionState.kt (data class with phase, progress, phaseDuration, remainingTime, totalCycleProgress, isPaused, cycle).
- Create ExerciseSessionViewModel.kt (Coroutine-driven timer emitting StateFlow<SessionState>).

Please inspect `shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/` and identify the exact paths where these files should be created. Then write a handoff.md report with a concrete implementation strategy and the exact target paths. Once done, send a message to the caller with your findings and the path to your report.
