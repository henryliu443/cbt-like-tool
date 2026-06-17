# Handoff Report: Dynamic Model Refactor

## Observation
- `AIProvider.kt` contained `AIModel.entries` and constants which were statically defining the models.
- `DefaultModelFetcher.kt` (lines 77, 99, 126), `SettingsManager.kt` (lines 96, 147), `HistoryViewModel.kt` (line 99), and `ReframeUseCase.kt` (line 108) were querying `AIModel.entries` for lookups, which crashed or defaulted if a dynamically loaded model was used.
- `SettingsViewModel.kt` explicitly referenced `AIModel` static constants for defaults and initial state.
- `OpenAIService.kt` didn't have `reasoning_effort` nor `max_completion_tokens` on `ChatCompletionBody`, and always passed `temperature` and `maxTokens`.

## Logic Chain
1. Removed `AIModel.entries` and moved constants to `object FallbackModels`.
2. Updated all lookup sites (`DefaultModelFetcher`, `SettingsManager`, `HistoryViewModel`, `ReframeUseCase`) to search against `FallbackModels.entries`. If a model isn't found, it dynamically instantiates using `AIModel(provider, modelId, prettyGenericName(modelId))`.
3. Updated `SettingsViewModel.kt` to use `FallbackModels.get(this)` for fetching defaults and fallback models. Replaced explicit `AIModel.XXX` static references with `FallbackModels.XXX`.
4. Modified `ChatCompletionBody` in `OpenAIService.kt` to include nullable `temperature`, `maxTokens`, `maxCompletionTokens`, and `reasoningEffort`.
5. Adjusted `Json` configuration in `OpenAIService.kt` to use `explicitNulls = false` so that nullable fields are omitted when serialized.
6. Implemented `supportsReasoningEffort` for "o1" and "o3" prefixes, setting `reasoningEffort` based on `ThinkingTemplate.AnalysisDepth` and conditionally omitting `temperature` and `maxTokens` in favor of `maxCompletionTokens`.

## Caveats
- `DeepSeekService.kt` shares `ChatCompletionBody` but doesn't explicitly support `reasoning_effort`. Because we made `reasoningEffort` and `maxCompletionTokens` nullable, they naturally fall back to `null` and get excluded from JSON serialization.

## Conclusion
The application no longer relies on a static `AIModel.entries` registry, and successfully utilizes dynamically instantiated models while resolving fallbacks correctly. `OpenAIService.kt` appropriately encapsulates and formats request parameters for `reasoning_effort` capabilities.

## Verification Method
1. Run `./gradlew assembleDebug` to verify successful compilation.
2. Run `grep -r "AIModel.entries" .` (excluding `.agents`) to verify there are 0 results remaining.
