# BRIEFING — 2026-06-05T17:00:15Z

## Mission
Investigate and plan the "Dynamic Model Refactor" milestone.

## 🔒 My Identity
- Archetype: Teamwork explorer
- Roles: Read-only investigation, analysis, synthesis, reporting
- Working directory: /Users/henry/cbt-like-tool/.agents/teamwork_preview_explorer_dynamic_model_1/
- Original parent: 9a926e24-9c5e-450a-a202-074b1d2db9f3 (main agent)
- Milestone: Dynamic Model Refactor

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Scope boundaries: AIProvider.kt, DefaultModelFetcher.kt, SettingsManager.kt, HistoryViewModel.kt, OpenAIService.kt, DeepSeekService.kt
- Remove static model registry (AIModel.entries). Add FallbackModels.
- Ensure dynamic instantiation of models across the app without throwing Unknown Model errors.
- Encapsulate reasoning_effort handling in OpenAIService and DeepSeekService.

## Current Parent
- Conversation ID: 9a926e24-9c5e-450a-a202-074b1d2db9f3
- Updated: not yet

## Investigation State
- **Explored paths**: `AIProvider.kt`, `SettingsManager.kt`, `DefaultModelFetcher.kt`, `SettingsViewModel.kt`, `HistoryViewModel.kt`, `OpenAIService.kt`, `DeepSeekService.kt`, `MoonshotService.kt`.
- **Key findings**: `AIModel.entries` is used in multiple places as a fallback and crashes/defaults to static if missing. `ChatCompletionBody` is shared and needs `reasoning_effort` added and `explicitNulls = false` config on JSON serializer to avoid sending `null` fields. `o1` and `o3` prefix can be used to dynamically toggle `reasoning_effort`.
- **Unexplored areas**: None

## Key Decisions Made
- Proposed moving static models to `object FallbackModels`.
- Proposed using dynamic `AIModel(..., id, prettyName(id))` initialization in ViewModels/Managers.
- Proposed updating `ChatCompletionBody` with `explicitNulls = false` json config to safely drop `max_tokens` for reasoning models.

## Artifact Index
- /Users/henry/cbt-like-tool/.agents/teamwork_preview_explorer_dynamic_model_1/BRIEFING.md — My persistent working memory
- /Users/henry/cbt-like-tool/.agents/teamwork_preview_explorer_dynamic_model_1/handoff.md — Final investigation and handoff report
