# Project: Pragmatic Adapter Refactor

## Architecture
- `AIProvider.kt`: Remove static model registry (`AIModel.entries`). Add `FallbackModels`.
- `DefaultModelFetcher.kt`, `SettingsManager.kt`, `HistoryViewModel.kt`: Dynamically instantiate models.
- `OpenAIService.kt`, `DeepSeekService.kt`: Encapsulate reasoning_effort handling.

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | Dynamic Model Refactor | `AIProvider.kt`, `DefaultModelFetcher.kt`, `SettingsManager.kt`, `HistoryViewModel.kt`, `OpenAIService.kt`, `DeepSeekService.kt` | none | IN_PROGRESS |

## Interface Contracts
- `AIModel(provider, modelName, displayName)` should be created dynamically instead of looking up an enum/registry.

## Code Layout
- Android/Kotlin Application
- `app/src/main/java/...`
- `shared/src/commonMain/kotlin/...` (likely location for some of these files)
