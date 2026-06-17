# Dynamic Model Refactor — Investigation Report

## 1. Observation

- **AIProvider.kt**: Contains the static model registry `AIModel.entries` and constants (e.g., `DEEPSEEK_CHAT`) within the `AIModel` companion object.
- **DefaultModelFetcher.kt, SettingsManager.kt**: Already instantiate models dynamically but rely on matching against `AIModel.entries` as a fallback.
- **HistoryViewModel.kt**: Uses `com.henryliu.cbtreframe.shared.AIModel.entries.firstOrNull` on line 99 and falls back to `AIModel.GPT_4O_MINI` instead of dynamically instantiating.
- **OpenAIService.kt**: `ChatCompletionBody` lacks a `reasoning_effort` field, and `max_tokens` / `temperature` are not adjusted dynamically based on reasoning capabilities.
- **DeepSeekService.kt**: Uses the shared `ChatCompletionBody` from `OpenAIService.kt` but lacks reasoning capability checks.
- **Out of Scope Files (Caveat)**: Found occurrences of `AIModel.entries` and `AIModel.<CONSTANT>` in `SettingsViewModel.kt` (lines 349-378) and `ReframeUseCase.kt` (line 108).

## 2. Logic Chain

1. **Remove Static Registry**: The constants and `entries` in `AIModel` must be moved to an `object FallbackModels`. This removes the static registry from `AIModel`.
2. **Dynamic Instantiation**: 
   - `DefaultModelFetcher.kt` and `SettingsManager.kt` must be updated to use `FallbackModels.entries` instead of `AIModel.entries`.
   - `HistoryViewModel.kt` must be changed to `FallbackModels.entries.firstOrNull { ... } ?: AIModel(provider, modelRaw, prettyGenericName(modelRaw))` to satisfy the requirement of supporting unknown models without throwing errors.
3. **Capability Encapsulation**: 
   - Update `ChatCompletionBody` in `OpenAIService.kt` to make `temperature` and `maxTokens` nullable, and add `@SerialName("reasoning_effort") val reasoningEffort: String? = null`.
   - Implement `private fun supportsReasoningEffort(modelName: String)` in both `OpenAIService.kt` (returns true for `o1` and `o3`) and `DeepSeekService.kt` (returns false).
   - In both services, map `depth` to `low`, `medium`, `high` if `supportsReasoningEffort` is true. Set `maxTokens = null` and `temperature = null` for supported models, otherwise leave as default.
4. **Compilation Requirement**: The acceptance criteria demand that "Code compiles successfully". If we only modify the scoped files, compilation will fail because `SettingsViewModel.kt` and `ReframeUseCase.kt` still reference `AIModel.entries` and `AIModel` constants. The implementing agent must also modify these out-of-scope files.

## 3. Caveats

- **Scope Boundary Conflict**: `SettingsViewModel.kt` and `ReframeUseCase.kt` are not listed in the "Files to modify" boundaries but contain references to `AIModel.entries` and static constants. Modifying them is **mandatory** to satisfy the "Compilation & Logic" acceptance criterion. The implementing agent should confidently fix these files to prevent broken builds.
- **DeepSeek parameters**: DeepSeek models currently do not support `reasoning_effort`, so `supportsReasoningEffort` will return false in `DeepSeekService.kt`.

## 4. Conclusion

**Proposed Fix Strategy for Implementing Agent**:
1. **AIProvider.kt**: Move all model constants and `val entries` from `AIModel` companion object to a new `object FallbackModels`.
2. **Update references**: Replace `AIModel.entries` with `FallbackModels.entries` in `DefaultModelFetcher.kt` and `SettingsManager.kt`.
3. **HistoryViewModel.kt**: Change `val model = ...` assignment in `sendFollowUpMessage` to dynamically instantiate with `prettyGenericName` if not found in `FallbackModels`.
4. **Adapter Parameters**: Add nullable `reasoningEffort`, `temperature`, and `maxTokens` to `ChatCompletionBody` in `OpenAIService.kt`.
5. **Logic Encapsulation**: Implement `supportsReasoningEffort(modelName: String)` in `OpenAIService.kt` and `DeepSeekService.kt`. Calculate `reasoningEffortVal` and conditionally pass it and `maxTokens` to `ChatCompletionBody` in all `reframe`, `streamReframe`, and `analyzeThoughtPatterns` functions.
6. **Out-of-Scope Files**: Update `SettingsViewModel.kt` and `ReframeUseCase.kt` to use `FallbackModels` to ensure successful compilation.

## 5. Verification Method

- **Compilation**: Run `./gradlew assembleDebug` to confirm all references to `AIModel.entries` are removed and the app compiles.
- **Test Unknown Model**: Instantiate an unknown model (e.g. `henry-super-model-v999`) in `HistoryViewModel` and ensure it uses `prettyGenericName` instead of throwing an error or falling back to a fixed model.
- **Code Inspection**: Review `ChatCompletionBody` in API requests for `o3-mini` to ensure `reasoning_effort` is present and `max_tokens` is omitted.
