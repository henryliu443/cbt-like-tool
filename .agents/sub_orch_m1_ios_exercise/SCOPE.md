# Scope: iOS Native Exercise Interface

## Architecture
- iOS native application structure, using KMP Shared logic via `ExerciseSessionViewModel`.
- `ExerciseSessionView.swift`: Subscribes to KMP `StateFlow<SessionState>`, parses state and passes down to Renderer and Haptic Engine.
- `FluidBreathingRenderer.swift`: SwiftUI Canvas based view.
- `AdvancedHapticEngine.swift`: CoreHaptics engine, initialized once and using `sendParameters`.
- `ExerciseLiveActivity.swift`: ActivityKit component for Lock Screen/Dynamic Island.

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | iOS Native Exercise Interface | Implement `AdvancedHapticEngine.swift`, `FluidBreathingRenderer.swift`, `ExerciseSessionView.swift`, `ExerciseLiveActivity.swift`, update `.pbxproj`, verify compilation. | none | DONE |

## Interface Contracts
### iOS View ↔ KMP ViewModel
- Subscribes to `StateFlow<SessionState>` from KMP.
