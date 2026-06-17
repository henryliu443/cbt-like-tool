# Project: CBTReframe Exercise iOS
# Scope: Exercise iOS Native Rendering and Haptics

## Architecture
- `AdvancedHapticEngine.swift` (CoreHaptics, manages `CHHapticPatternPlayer`, continuous modulation based on phase/progress)
- `FluidBreathingRenderer.swift` (SwiftUI Canvas, multiple radial gradients, noise/randomization)
- `ExerciseSessionView.swift` (Single source of truth, subscribes to KMP `StateFlow<SessionState>`, orchestrates Haptic & Visuals)
- `ExerciseLiveActivity.swift` (ActivityKit, lock screen / dynamic island)

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | Haptics & Visuals | Implement `AdvancedHapticEngine` and `FluidBreathingRenderer` | none | DONE |
| 2 | Session & Activity | Implement `ExerciseSessionView`, integrate it, and `ExerciseLiveActivity` | M1 | DONE |

## Interface Contracts
### `ExerciseSessionView` ↔ `AdvancedHapticEngine`
- `ExerciseSessionView` reads `SessionState` and calls `AdvancedHapticEngine.shared.sendParameters(phase:progress:)` (or similar).

### `ExerciseSessionView` ↔ `FluidBreathingRenderer`
- `FluidBreathingRenderer(phase: phase, progress: progress)`

### KMP `ExerciseSessionViewModel` ↔ `ExerciseSessionView`
- KMP provides `StateFlow<SessionState>` that `ExerciseSessionView` collects.
