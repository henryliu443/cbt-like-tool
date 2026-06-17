# Original User Request

## Initial Request — 2026-06-05T08:54:50Z

# Teamwork Project Prompt — Draft

> Status: Launched
> Goal: Craft prompt → get user approval → delegate to teamwork_preview

Implement a Pragmatic Adapter pattern in CBT Reframe to decouple the AI model definitions from a static registry. Remove the hardcoded `AIModel.entries` list, ensure dynamic instantiation of models across the app, and localize CBT-specific depth abstractions (Fast/Balanced/Deep) into vendor-specific API parameters (like reasoning_effort) directly within the service adapters.

Working directory: ~/cbt-like-tool
Integrity mode: development

## Requirements

### R1. Remove Static Model Registry
Delete `AIModel.entries` from `AIProvider.kt`. Move existing constants to a `FallbackModels` object to be used only as fallback defaults.

### R2. Dynamic Model Instantiation
Refactor `DefaultModelFetcher.kt`, `SettingsManager.kt`, and `HistoryViewModel.kt` to dynamically instantiate `AIModel(provider, modelName, displayName)` instead of matching against the static registry. The system must support entirely unknown models without throwing `Unknown Model` errors.

### R3. Adapter-Level Capability Encapsulation & Parameter Mapping
In `OpenAIService.kt` and `DeepSeekService.kt`, encapsulate capability detection (e.g., `supportsReasoningEffort(modelName)`).
- For models supporting `reasoning_effort` (like `o1`, `o3-mini`), map `AnalysisDepth.Fast` to `low`, `Balanced` to `medium`, and `Deep` to `high`.
- For models that do not support reasoning parameters, do not alter `max_tokens` or inject fake reasoning parameters. Rely purely on the prompt structure for depth control.

## Acceptance Criteria

### Compilation & Logic
- [ ] Code compiles successfully (`./gradlew assembleDebug` completes with no errors).
- [ ] No occurrences of `AIModel.entries` remain in the codebase.

### Edge Case Resilience
- [ ] If an unknown model name (e.g. `henry-super-model-v999`) is passed to `HistoryViewModel`, it instantiates correctly and does not crash.

## 2026-06-05T09:03:17Z

Please review AGENT.md before you finalize your work. You are the Orchestrator. When your team finishes modifying code and compiling, you MUST NOT declare the task complete yourself. Instead, you must report that the code modifications and compile report are ready, so that the parent agent (PM) can submit it to Critic (Codex) according to the newly updated AGENT.md workflow.

## 2026-06-14T05:06:31Z

Redesign the Exercise interface by implementing a Kotlin Multiplatform (KMP) state machine for session logic and a high-fidelity iOS SwiftUI rendering layer with CoreHaptics and Canvas gradient animations. Android rendering is explicitly out of scope for now.

Working directory: /Users/henry/cbt-like-tool
Integrity mode: development

## Constraints from AGENT.md
- **Minimize Token Usage**: No unnecessary pleasantries. Use partial file edits instead of full rewrites. Provide concise stacktraces on failures.
- **Strict Verification**: The Builder agent must include boundary testing and error handling. "Works on my machine" is rejected.
- **Red/Blue Dynamics**: The work must undergo rigorous review (Critic) before being considered passed.

## Requirements

### R1. KMP Shared State Machine
Implement the exercise session logic in `shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/session/`.
- Create `ExercisePhase.kt` (enum for Inhale, Hold, Exhale, Rest).
- Create `ExerciseDefinition.kt` (configurable PhaseSequence).
- Create `ExerciseSessionState.kt` (data class with phase, progress, phaseDuration, remainingTime, totalCycleProgress, isPaused, cycle).
- Create `ExerciseSessionViewModel.kt` (Coroutine-driven timer emitting `StateFlow<SessionState>`).

### R2. iOS Native CoreHaptics Engine
Implement `AdvancedHapticEngine.swift` in the iOS project (`CBTReframe/Haptic/`).
- It must **not** subscribe directly to the KMP StateFlow.
- It should manage a long-lived `CHHapticPatternPlayer`.
- It must update `CHHapticDynamicParameter` (Intensity and Sharpness) via `sendParameters` based on state updates passed to it.

### R3. iOS Visual Renderer
Implement `FluidBreathingRenderer.swift` using SwiftUI `Canvas`.
- It must render at least three layered radial gradients (`Core Glow`, `Soft Bloom`, `Ambient Ring`).
- It must include low-frequency random noise (opacity jittering or coordinate distortion) for an organic feel.
- It receives the state updates from the parent view.

### R4. iOS Container and Integration
- Implement `ExerciseSessionView.swift` to subscribe to the KMP `StateFlow<SessionState>`. This view is the single source of truth, passing state down to both the renderer and the haptic engine.
- Integrate this new view into the main app structure (e.g., replacing the current `ExercisesView` logic or linking to it).

### R5. Live Activity Support
Implement `ExerciseLiveActivity.swift` using ActivityKit to show exercise progress (phaseDuration, remainingTime) on the Lock Screen and Dynamic Island.

## Acceptance Criteria

### KMP Engine
- [ ] `ExerciseSessionViewModel` successfully emits state updates at a regular interval via `StateFlow` without crashing.
- [ ] The state correctly transitions through the defined `PhaseSequence`.

### iOS Execution & Haptics
- [ ] `ExerciseSessionView` successfully consumes the KMP `StateFlow`.
- [ ] The `AdvancedHapticEngine` does not recreate the pattern player on every frame; it successfully uses `sendParameters` to modulate continuous vibration.
- [ ] The app builds and runs without runtime crashes when initiating an exercise session.

### iOS Visuals
- [ ] The `FluidBreathingRenderer` compiles and renders multiple radial layers within a `Canvas`.
- [ ] A noise or randomization factor is programmatically present in the renderer's logic.

### Live Activity
- [ ] `ExerciseLiveActivity` is properly defined and configured in `Info.plist` (if required) and can be started/updated based on session state.
