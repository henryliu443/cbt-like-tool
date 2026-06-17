## Consensus
All required files should be located in `/Users/henry/cbt-like-tool/shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/`.
- `ExercisePhase.kt` exists but needs to be updated with `Inhale, Hold, Exhale, Rest`.
- `ExerciseDefinition.kt` must be created.
- `ExerciseSessionState.kt` must be created.
- `ExerciseSessionViewModel.kt` must be created with Coroutine-driven StateFlow logic.

## Strategy
- Implement `ExerciseSessionState` as a data class holding cycle, progress, time remaining, etc.
- Implement `ExerciseSessionViewModel` utilizing KMP coroutines to manage time and phase transitions.
