# Scope: M2 iOS Implementation

## Architecture
- iOS App (`CBTReframe/`) containing UI and Haptics.
- Haptic Engine (`AdvancedHapticEngine.swift`) driven by state updates.
- Visual Renderer (`FluidBreathingRenderer.swift`) with SwiftUI Canvas and gradient animations.
- Container (`ExerciseSessionView.swift`) bridging the state flow to haptic and renderer.
- Live Activity (`ExerciseLiveActivity.swift`).

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | M2 iOS Implementation | `AdvancedHapticEngine.swift`, `FluidBreathingRenderer.swift`, `ExerciseSessionView.swift`, `ExerciseLiveActivity.swift` | M1 | PLANNED |

## Interface Contracts
### KMP ↔ iOS
- `ExerciseSessionViewModel` exposes `StateFlow<ExerciseSessionState>`.
- `ExerciseSessionState` contains properties: `phase` (enum for Inhale, Hold, Exhale, Rest), `progress` (float), `phaseDuration` (long), `remainingTime` (long), `totalCycleProgress` (float), `isPaused` (boolean), `cycle` (int).
