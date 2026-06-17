# Handoff Report: Dynamic Model Refactor

## Observation
- `AIProvider.kt` currently defines a static `AIModel.entries` list which is used as a global registry.
- `DefaultModelFetcher.kt`, `SettingsManager.kt`, and `HistoryViewModel.kt` look up models via `AIModel.entries.firstOrNull`.
- In `HistoryViewModel.kt`, if an unknown dynamic model is used, it throws/fails and falls back to `GPT_4O_MINI` because it only checks the static `entries` list.
- `OpenAIService.kt` defines `ChatCompletionBody` which is also used by `DeepSeekService.kt`. `ChatCompletionBody` currently lacks support for `reasoning_effort` and `max_completion_tokens`.
- `OpenAIService.kt` explicitly configures `Json { ignoreUnknownKeys = true; isLenient = true }` but does not omit explicit nulls.

## Logic Chain
1. **Remove static registry & Add FallbackModels**: By removing `AIModel.entries` and introducing `object FallbackModels`, we force all components to use provider-specific fallbacks dynamically.
2. **Fix dynamic instantiation**: To prevent Unknown Model errors in `HistoryViewModel` (and similarly in `DefaultModelFetcher`/`SettingsManager`), whenever a model ID is not found in the fallback list, we instantiate it dynamically: `AIModel(provider, id, prettyGenericName(id))`.
3. **Encapsulate reasoning_effort**: 
   - We must add `@SerialName("reasoning_effort") val reasoningEffort: String? = null` and `@SerialName("max_completion_tokens") val maxCompletionTokens: Int? = null` to `ChatCompletionBody`.
   - Update `temperature` and `maxTokens` to be nullable (`Double?` and `Int?`).
   - Add `explicitNulls = false` to the `Json` instance in `OpenAIService.kt` so that when these fields are `null`, they are completely omitted from the network request payload (avoiding fake parameters for models that don't support them, like standard DeepSeek or GPT-4o models).
   - In `OpenAIService.kt`, determine if the model supports reasoning effort (e.g., `modelName.startsWith("o1") || modelName.startsWith("o3-mini")`). If so, map `depth` to `low`/`medium`/`high` for `reasoningEffort`, move the token count to `maxCompletionTokens`, and pass `temperature = null` and `maxTokens = null`.
   - `DeepSeekService.kt` will use the updated `ChatCompletionBody` but leave `reasoningEffort` and `maxCompletionTokens` as `null`, naturally avoiding fake parameter injection.

## Caveats
- `SettingsViewModel.kt` already contains an extension `fun AIProvider.fallbackModels(): List<AIModel>`. We could either reuse this extension or define `object FallbackModels` in `AIProvider.kt` to consolidate. I propose creating `object FallbackModels` in `AIProvider.kt` and optionally migrating `SettingsViewModel` to use it (though `SettingsViewModel` is outside the strictly defined scope boundaries).
- `ReframeUseCase.kt` also references `AIModel.entries`. While outside the explicit scope boundary, it will fail to compile if `entries` is removed. It must be updated to use `FallbackModels.get(provider)` and dynamic instantiation.

## Conclusion
The refactor should proceed by:
1. Deleting `AIModel.entries` and creating `object FallbackModels { fun get(provider: AIProvider): List<AIModel> = ... }` in `AIProvider.kt`.
2. Updating `HistoryViewModel.kt`, `SettingsManager.kt`, and `DefaultModelFetcher.kt` to use `FallbackModels.get(provider)` and `AIModel(provider, id, prettyGenericName(id))` for unknown models.
3. Updating `ChatCompletionBody` and the `Json` configuration in `OpenAIService.kt` to support optional properties without encoding `null`s.
4. Injecting `reasoning_effort` (mapped from depth) and `max_completion_tokens` specifically for `o1` and `o3-mini` in `OpenAIService.kt`.

## Verification Method
- Build the project using `./gradlew assembleDebug` (or standard KMP build) to ensure no compilation errors after removing `AIModel.entries`.
- Run tests (`./gradlew test`) or launch the app to verify model fetching works.
- Check network interceptor or proxy to ensure `reasoning_effort` is sent for `o3-mini` but absent for `gpt-4o` and `deepseek-chat`.
