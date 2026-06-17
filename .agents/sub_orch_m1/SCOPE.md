# Scope: M1 KMP State Machine

## Architecture
- KMP Shared Session Module (`shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/`) for state machine logic.
- Coroutine-driven timer emitting `StateFlow<ExerciseSessionState>`.

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | M1 KMP Implementation | `ExercisePhase.kt`, `ExerciseDefinition.kt`, `ExerciseSessionState.kt`, `ExerciseSessionViewModel.kt` | none | PLANNED |

## Interface Contracts
### KMP ↔ iOS
- `ExerciseSessionViewModel` exposes `StateFlow<ExerciseSessionState>`.
- `ExerciseSessionState` contains properties: `phase` (enum for Inhale, Hold, Exhale, Rest), `progress` (float), `phaseDuration` (long), `remainingTime` (long), `totalCycleProgress` (float), `isPaused` (boolean), `cycle` (int).
