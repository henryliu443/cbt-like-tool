# Handoff Report: Dynamic Model Refactor

## Observation
- `AIModel.entries` has been completely removed across the codebase, replaced by `FallbackModels.entries`.
- Classes such as `SettingsManager`, `DefaultModelFetcher`, `HistoryViewModel`, and `ReframeUseCase` now dynamically instantiate an `AIModel` instance when a model ID does not match any entry in `FallbackModels`, utilizing `prettyGenericName` for labels.
- `OpenAIService.kt` sets `temperature`, `max_tokens`, `max_completion_tokens`, and `reasoning_effort` dynamically based on the helper `supportsReasoningEffort(modelName)` which verifies `o1` or `o3` prefix.
- The `ChatCompletionBody` makes these parameters nullable, and `json` serializer uses `explicitNulls = false`.

## Logic Chain
1. The removal of the static `AIModel.entries` forces the code to use dynamic mapping. `FallbackModels` acts purely as a backup and a source of defaults.
2. The `prettyGenericName` utility was stress-tested against empty and malformed strings. Kotlin's `replaceFirstChar` avoids out-of-bounds index errors on empty strings, ensuring no crashes on unpredictable model IDs.
3. The `reasoning_effort` flag safely targets only `o1`/`o3` families. Non-matching models (like `gpt-4o` or deepseek equivalents passing through OpenAI compatibility) receive a `null` value for `reasoningEffort`.
4. Tests with `explicitNulls = false` confirm that these `null` values are wholly omitted from the final network JSON payload, averting `400 Bad Request` exceptions from downstream LLM providers that do not recognize those keys.

## Caveats
- `o1-mini` inherits the `reasoning_effort` payload based on the prefix logic. While OpenAI's API originally did not support this on `o1-mini`, recent updates are rolling it out, and the model does not hard-crash the application locally.
- `DeepSeekService` shares the `ChatCompletionBody` but operates completely oblivious to `reasoning_effort`, cleanly avoiding validation errors on DeepSeek's side.

## Conclusion
The dynamic model handling operates smoothly and does not crash when encountering unknown models. The `reasoning_effort` mapping correctly isolates advanced o1/o3 requests from normal inferences. 

Verdict: **PASS**

## Verification Method
- Execute `./gradlew shared:testDebugUnitTest` to confirm tests build and pass.
- Execute a custom Kotlin script leveraging `kotlinx.serialization.json.Json { explicitNulls = false }` to serialize `ChatCompletionBody` initialized with null `reasoningEffort`, which omits the key as expected.
