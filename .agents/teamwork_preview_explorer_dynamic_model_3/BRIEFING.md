# BRIEFING — 2026-06-05T16:59:31+08:00

## Mission
Investigate and plan the "Dynamic Model Refactor" milestone for the CBT-like app.

## 🔒 My Identity
- Archetype: Teamwork explorer
- Roles: Read-only investigator
- Working directory: /Users/henry/cbt-like-tool/.agents/teamwork_preview_explorer_dynamic_model_3/
- Original parent: 9a926e24-9c5e-450a-a202-074b1d2db9f3
- Milestone: Dynamic Model Refactor

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Network mode: CODE_ONLY (local filesystem search and view_file only)

## Current Parent
- Conversation ID: 9a926e24-9c5e-450a-a202-074b1d2db9f3
- Updated: 2026-06-05T16:59:31+08:00

## Investigation State
- **Explored paths**: AIProvider.kt, DefaultModelFetcher.kt, SettingsManager.kt, HistoryViewModel.kt, OpenAIService.kt, DeepSeekService.kt, ReframeUseCase.kt, SettingsViewModel.kt.
- **Key findings**: 
  - `AIModel.entries` is used globally and causes Unknown Model fallback if dynamic models are selected.
  - `ChatCompletionBody` is shared between `OpenAIService` and `DeepSeekService`.
  - kotlinx.serialization needs `explicitNulls = false` to avoid injecting fake `null` fields to APIs that do not support them.
- **Unexplored areas**: None.

## Key Decisions Made
- Replace `AIModel.entries` with `object FallbackModels`.
- Use dynamic `AIModel` instantiation for fallback resolution in `HistoryViewModel` and others.
- Update `ChatCompletionBody` with nullable fields and `explicitNulls = false` so `DeepSeekService` drops unsupported parameters naturally while `OpenAIService` injects them for `o1`/`o3` models.

## Artifact Index
- /Users/henry/cbt-like-tool/.agents/teamwork_preview_explorer_dynamic_model_3/handoff.md — Analysis and proposed fix strategy
