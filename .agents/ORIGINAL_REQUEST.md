# Original User Request

## Initial Request — 2026-06-06T13:58:35Z

# Teamwork Project Prompt

> Status: Launched
> Goal: Fix remaining bugs and UI alignment issues identified in the code review.

Fix the remaining bugs and UI alignment issues identified in the code review. This includes adding missing exception handlers in `FollowUpChatView`, removing dead code, and standardizing Design Tokens across `HomeView` and related components.

Working directory: /Users/henry/cbt-like-tool
Integrity mode: demo

## Requirements

### R1. Fix FollowUpChatView Exception Handling
Add a `catch (e: Exception)` block to the coroutine calling `sendFollowUpMessage()` in `FollowUpChatView.kt` to prevent app crashes when API Key is missing. Update the UI to reflect the error state instead of crashing.

### R2. Remove Dead Code
Delete `shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/AIService.kt` and `shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/services/AIServiceImpl.kt`, and remove any Koin bindings referencing them.

### R3. Standardize Design Tokens for UI Alignment
Refactor `HomeView.kt` and related components to use unified icon sizes and typography:
- Section Header icons: 20dp, typography: `labelMedium` + SemiBold
- Dashboard icons: 20dp
- Banner icons: 18dp
- Action bar icons: 16dp
- QuickStart / MoodPicker: Replace Emojis with Material Icons to ensure strict vertical alignment across devices. Do not use Emojis for these UI components.

## Acceptance Criteria

### Code Quality
- [ ] `AIService.kt` and `AIServiceImpl.kt` are successfully deleted.
- [ ] `FollowUpChatView` handles `AIServiceError.NoAPIKey` properly without throwing unhandled exceptions.
- [ ] `./gradlew :app:assembleDebug` builds successfully.

### UI Standardization
- [ ] Programmatic checks (e.g., via `grep`) confirm no ad-hoc icon sizes (12dp, 14dp, 19dp, 22dp, 28dp) remain in `HomeView` and `ResultCardView` for the standardized elements.
- [ ] Programmatic checks confirm that Emojis have been replaced by Material Icons in `QuickStart` and `MoodPicker`.

## Follow-up — 2026-06-15T12:25:57Z

Complete the Exercise interface redesign by implementing the iOS native rendering and haptic components. Milestone 1 (KMP state machine) is already complete and available in `shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/`.

Working directory: /Users/henry/cbt-like-tool
Integrity mode: development

## Constraints from AGENT.md
- **Minimize Token Usage**: No unnecessary pleasantries. Use partial file edits instead of full rewrites. Provide concise stacktraces on failures.
- **Strict Verification**: The Builder agent must include boundary testing and error handling. "Works on my machine" is rejected.
- **Red/Blue Dynamics**: The work must undergo rigorous review (Critic) before being considered passed.

## Requirements

### R1. iOS Native CoreHaptics Engine
Implement `AdvancedHapticEngine.swift` in the iOS project (`CBTReframe/Haptic/`).
- It must **not** subscribe directly to the KMP StateFlow.
- It should manage a long-lived `CHHapticPatternPlayer` initialized at session start.
- It must update `CHHapticDynamicParameter` (Intensity and Sharpness) via `sendParameters` dynamically based on state updates (phase and progress) passed to it by its parent view.

### R2. iOS Visual Renderer
Implement `FluidBreathingRenderer.swift` using SwiftUI `Canvas`.
- It must render at least three layered radial gradients (`Core Glow`, `Soft Bloom`, `Ambient Ring`) using `GraphicsContext.Shading.radialGradient`.
- It must include low-frequency random noise (opacity jittering or slight coordinate distortion) for an organic, breathing halo feel.
- It receives the state updates (phase, progress) from the parent view.

### R3. iOS Container and Integration
- Implement `ExerciseSessionView.swift` to subscribe to the KMP `StateFlow<SessionState>` exposed by `ExerciseSessionViewModel`. This view acts as the single source of truth, parsing state and passing it down to both `FluidBreathingRenderer` and `AdvancedHapticEngine`.
- Integrate this new view into the main app structure (`CBTReframeApp.swift` or replacing the current `ExercisesView` routing logic).

### R4. Live Activity Support
Implement `ExerciseLiveActivity.swift` using ActivityKit to show exercise progress (`phaseDuration`, `remainingTime`) on the Lock Screen and Dynamic Island.

## Acceptance Criteria

### iOS Execution & Haptics
- [ ] `ExerciseSessionView` successfully observes and decodes the KMP `StateFlow`.
- [ ] The `AdvancedHapticEngine` does not recreate the pattern player on every frame; it successfully uses `sendParameters` to continuously modulate vibration based on the phase progress.
- [ ] The app builds and runs without runtime crashes when navigating to and running an exercise session.

### iOS Visuals
- [ ] The `FluidBreathingRenderer` compiles and renders multiple radial layers within a `Canvas`.
- [ ] Randomization/noise logic is successfully implemented in the renderer's drawing cycle.

### Live Activity
- [ ] `ExerciseLiveActivity` is defined, conforms to ActivityKit protocols, and updates correctly based on session state changes.
