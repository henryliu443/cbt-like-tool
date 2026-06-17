# Dynamic Model Refactor — Investigation & Handoff Report

## 1. Observation
- `AIProvider.kt` currently defines `AIModel` with a `companion object` that holds both static model `val` declarations (like `GPT_4O`) and a static `entries` list.
- `SettingsManager.kt` (lines 96, 147), `DefaultModelFetcher.kt` (lines 77, 99, 126), and `HistoryViewModel.kt` (line 99) rely on `AIModel.entries.firstOrNull` to resolve model metadata. If a cached or stored model ID isn't in `entries`, it can cause nulls or fallbacks to incorrect models (like `GPT_4O_MINI`), preventing dynamic model resolution.
- `SettingsViewModel.kt` defines extension methods `AIProvider.fallbackModels()` and `AIProvider.defaultModelId()`.
- `OpenAIService.kt` and `DeepSeekService.kt` use a shared `ChatCompletionBody` (defined in `OpenAIService.kt`) that lacks the `reasoning_effort` property. They currently hardcode `max_tokens` and `temperature` for all models.
- The `json` serializer in `OpenAIService.kt` uses `Json { ignoreUnknownKeys = true; isLenient = true }`, which by default does not emit `explicitNulls = false`.

## 2. Logic Chain
1. **Remove static model registry:** We need to delete the `AIModel` companion object's `entries` list. We should move the static model `val`s into a new `object FallbackModels` in `AIProvider.kt`.
2. **Move extensions:** We should move `fallbackModels()` and `defaultModelId()` from `SettingsViewModel.kt` into `AIProvider.kt` (or as methods inside `FallbackModels`).
3. **Dynamic Instantiation:** Replace all `AIModel.entries.firstOrNull` lookups in `HistoryViewModel.kt`, `SettingsManager.kt`, and `DefaultModelFetcher.kt` with a dynamic instantiation pattern:
   ```kotlin
   FallbackModels.getModels(provider).firstOrNull { it.modelName == id } 
       ?: AIModel(provider, id, prettyGenericName(id))
   ```
   This ensures we never throw "Unknown Model" and can seamlessly support any model ID returned from backend APIs or saved in DB.
4. **Reasoning Effort Handling:**
   - Update `ChatCompletionBody` to make `temperature` and `maxTokens` nullable, and add `reasoningEffort: String? = null`.
   - Update `json` in `OpenAIService.kt` to include `explicitNulls = false` so that nullable fields are omitted entirely when `null`.
   - In `OpenAIService` and `DeepSeekService`, check `val supportsReasoning = model.modelName.startsWith("o1") || model.modelName.startsWith("o3")`.
   - If it supports reasoning, set `reasoningEffort = when(depth) { fast -> "low", balanced -> "medium", deep -> "high" }`, and set `maxTokens = null`, `temperature = null`.
   - If it does not, pass `reasoningEffort = null` and retain the original `maxTokens` (e.g. 1024 or 2048) and `temperature` logic.

## 3. Caveats
- DeepSeek's `deepseek-reasoner` currently ignores `temperature` and handles reasoning implicitly, it doesn't officially support the exact `reasoning_effort` JSON parameter. However, encapsulating the logic in both services ensures compatibility if OpenAI-compatible routers are used or if DeepSeek adds support. We will apply the same `o1`/`o3` prefix check, so `deepseek-reasoner` will gracefully fall back to the standard flow without `reasoning_effort` unless we explicitly map it.
- `MoonshotService` also uses `ChatCompletionBody`. The changes to `ChatCompletionBody` with nullable properties and omitted nulls won't break Moonshot since it explicitly passes `temperature` and `maxTokens`.

## 4. Conclusion & Proposed Strategy
- **AIProvider.kt**: Create `object FallbackModels` holding all static models. Move `fallbackModels()` list here. Remove `AIModel.entries`.
- **HistoryViewModel.kt**: Update line 99 to use `FallbackModels.getModels(provider).firstOrNull { ... } ?: AIModel(...)`.
- **SettingsManager.kt**: Update lines 96 & 147 similarly.
- **DefaultModelFetcher.kt**: Update lines 77, 99, 126 similarly.
- **OpenAIService.kt**: 
  - Update `ChatCompletionBody` with `temperature: Double? = null`, `maxTokens: Int? = null`, `@SerialName("reasoning_effort") val reasoningEffort: String? = null`.
  - Add `explicitNulls = false` to `json`.
  - In `reframe` and `streamReframe`, map `depth` to `reasoning_effort` for `o1`/`o3` models. Set `maxTokens` and `temperature` to `null` for these models.
- **DeepSeekService.kt**: 
  - Replicate the same `supportsReasoning` check and `reasoningEffort` mapping when instantiating `ChatCompletionBody`.

## 5. Verification Method
- **Static checks:** Search for `AIModel.entries` across the codebase; it should return 0 results.
- **Build test:** Run `./gradlew build` (or equivalent Kotlin Multiplatform build command, e.g. `./gradlew shared:compileKotlinIOSX64`) to ensure `HistoryViewModel`, `SettingsManager`, and `DefaultModelFetcher` compile successfully.
- **Behavior test:** Open the app or test the serialization to confirm that `ChatCompletionBody` for an `o1-mini` model correctly omits `max_tokens` and includes `reasoning_effort`.
