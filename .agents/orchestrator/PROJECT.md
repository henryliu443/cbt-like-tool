# Project: Exercise Redesign

## Architecture
- KMP Shared Session Module (`shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/`) for state machine logic and Coroutine-driven timer emitting `StateFlow<SessionState>`.
- iOS App (`CBTReframe/`) containing UI and Haptics.
  - Haptic Engine (`AdvancedHapticEngine.swift`) driven by state updates.
  - Visual Renderer (`FluidBreathingRenderer.swift`) with SwiftUI Canvas and gradient animations.
  - Container (`ExerciseSessionView.swift`) bridging the state flow to haptic and renderer.
  - Live Activity (`ExerciseLiveActivity.swift`).

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | KMP State Machine | `ExercisePhase`, `ExerciseDefinition`, `ExerciseSessionState`, `ExerciseSessionViewModel` | none | IN_PROGRESS |
| 2 | iOS Implementation | `AdvancedHapticEngine`, `FluidBreathingRenderer`, `ExerciseSessionView`, `ExerciseLiveActivity` | M1 | PLANNED |

## Interface Contracts
### KMP ↔ iOS
- `ExerciseSessionViewModel` exposes `StateFlow<ExerciseSessionState>`.
- `ExerciseSessionState` contains properties: `phase` (enum), `progress` (float), `phaseDuration` (long), `remainingTime` (long), `totalCycleProgress` (float), `isPaused` (boolean), `cycle` (int).

## Code Layout
- `shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/`
- `CBTReframe/Haptic/`
- `CBTReframe/Views/` (or similar iOS view structure)
