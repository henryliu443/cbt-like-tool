# Dynamic Model Refactor Instructions

## 1. Goal
Implement a Pragmatic Adapter pattern to decouple the AI model definitions from a static registry and dynamically instantiate models.

## 2. Requirements and Strategy

### Remove Static Registry
- **File**: `shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/AIProvider.kt`
- Delete `AIModel.entries`.
- Move the existing constants (e.g. `GPT_4O`, `CLAUDE_3_5_SONNET`) to a new `object FallbackModels`.
- Provide a way to get fallback models by provider (e.g., `FallbackModels.get(provider)` or `FallbackModels.entries`).

### Fix Dynamic Instantiation
- **Files**: `DefaultModelFetcher.kt`, `SettingsManager.kt`, `HistoryViewModel.kt` (and any other files like `SettingsViewModel.kt` or `ReframeUseCase.kt` if they break on compilation).
- Update to use `FallbackModels` instead of `AIModel.entries`.
- To support entirely unknown models without throwing errors, instantiate models dynamically if not found in fallbacks: `AIModel(provider, modelId, prettyGenericName(modelId))`.

### Encapsulate `reasoning_effort`
- **File**: `OpenAIService.kt`
- Update `ChatCompletionBody` to include `@SerialName("reasoning_effort") val reasoningEffort: String? = null` and `@SerialName("max_completion_tokens") val maxCompletionTokens: Int? = null`.
- Make `temperature` and `maxTokens` nullable (`Double?` and `Int?`).
- In the `Json` configuration inside `OpenAIService.kt`, add `explicitNulls = false` so null fields aren't serialized.
- Add capability detection: `private fun supportsReasoningEffort(modelName: String) = modelName.startsWith("o1") || modelName.startsWith("o3")`.
- When constructing `ChatCompletionBody`, if the model supports reasoning effort: map `AnalysisDepth.Fast` to `"low"`, `Balanced` to `"medium"`, `Deep` to `"high"`. Move the max token limit to `maxCompletionTokens` instead of `maxTokens`, and pass `null` for `temperature` and `maxTokens`.
- For models that do not support reasoning effort, pass the normal `temperature` and `maxTokens`, leaving `reasoningEffort` and `maxCompletionTokens` as `null`.
- **File**: `DeepSeekService.kt`
- It shares `ChatCompletionBody` but doesn't support `reasoning_effort` right now. Since `reasoningEffort` is nullable and we use `explicitNulls = false`, we can just leave it as `null`.

## 3. Mandatory Integrity Warning
> DO NOT CHEAT. All implementations must be genuine. DO NOT
> hardcode test results, create dummy/facade implementations, or
> circumvent the intended task. A Forensic Auditor will independently
> verify your work. Integrity violations WILL be detected and your
> work WILL be rejected.

## 4. Verification
- Code must compile successfully (`./gradlew assembleDebug` completes with no errors).
- No occurrences of `AIModel.entries` remain.
