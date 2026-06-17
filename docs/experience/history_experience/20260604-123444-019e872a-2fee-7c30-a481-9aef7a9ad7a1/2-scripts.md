# Scripts Experience

## Evidence Read
- Scope: matched session 019e872a-2fee-7c30-a481-9aef7a9ad7a1.
- Matched Codex sessions (exact cwd only): 019e872a-2fee-7c30-a481-9aef7a9ad7a1.
- Current repository facts discovered from the working directory.
- Existing repository files and directories that were already present before AGENTS governance scaffolding.

### Matched Session Files
- `/Users/henry/.codex/sessions/2026/06/02/rollout-2026-06-02T15-09-09-019e872a-2fee-7c30-a481-9aef7a9ad7a1.jsonl`

### Current Workspace Evidence
- `.DS_Store`
- `.agentmd`
- `.agents/agents-control.json`
- `.agents/design-interview-state.json`
- `.agents/docs-governance-state.json`
- `.agents/global-rule-overrides.json`
- `.agents/script-governance-exceptions.json`
- `.gitignore`
- `.gradle/8.13/checksums/checksums.lock`
- `.gradle/8.13/checksums/md5-checksums.bin`

### Session Excerpts
### Session `019e872a-2fee-7c30-a481-9aef7a9ad7a1`
- User: Please write `ReframeUseCase.kt` in `shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/`. Role: The topmost use case coordinating the reframe request. Dependencies (inject via constructor): `ReframeOrchestrator
- Assistant: Let me first explore the existing codebase to understand the patterns, types, and dependencies I'll need to work with.
- Assistant: Hmm, I see a significant mismatch between what the user described and the actual codebase APIs. Let me check a few more things to understand the real types:
- Assistant: Now I have the full picture. There are several mismatches between the user's description and the actual codebase: 1. `HistoryRepository.addHistory` takes `(id, originalThought, reframedThought, modelName, timestamp)` — n
- Assistant: There's `com.benasher44:uuid:0.8.2` available. Now I have everything I need. Let me create the file, adapting the user's intent to the real codebase: - `RiskRouting` is actually top-level functions, not an object — I'll 
- Assistant: Let me try `apply_patch` with the correct format:

## Task Context
- The current working directory already contained project content but did not yet contain a root `AGENTS.md`. This bootstrap experience records how the existing code and the exact-cwd Codex session history should be read together before the workspace is normalized.
- This `Scripts` record is generated from session evidence plus landed repository content so future agents can understand what work was underway, what files mattered, and what governance repairs were required.

## How To Apply
- First inspect whether the root `AGENTS.md` is missing and whether the workspace already contains meaningful files or directories.
- Then read only the Codex sessions whose `session_meta.payload.cwd` exactly matches the current working directory; do not mix in adjacent or similarly named workspaces.
- Use the matched sessions to reconstruct the recent user intent, combine that with the current file tree, and only then create the governed docs layout, latest experience files, and any required structure migration plan.
- If directory structure is not compliant, ask whether to normalize it before changing files. The recommended default is yes, but the confirmation must still be explicit.

## Problems And Risks
- If session matching is too broad, another repository's history can contaminate the current experience files and make the generated AGENTS guidance unsafe.
- If the workspace layout is normalized without asking first, the agent can silently move the wrong files or create governance output in the wrong place.
- If only the current file tree is used and the Codex session history is ignored, the resulting experience files can miss the real reason the repository is in its current partial state.

## Iterated Lessons
- `Scripts` bootstrap records should capture both the current repository evidence and the matching Codex session excerpts so later agents can understand not only what files exist, but why they exist and what restructuring or governance work was already being discussed.
- The bootstrap process should remain conservative: exact-cwd matching only, no silent directory normalization, and no fabricated narrative beyond what the file tree and the matched session excerpts support.
- User-visible paths should stay normalized and readable. Repository-relative evidence such as `src/main.py`, `docs/experience/1-workflow.md`, or `engineering/demo-app/` is easier to audit than collapsed raw path strings.

## Next Application
- Use this `Scripts` template when the next missing-AGENTS bootstrap needs to recover context from local Codex sessions and landed files before ordinary docs governance begins.
- If exact-cwd session evidence is missing, say that clearly and lean harder on file evidence instead of pretending the missing conversation details are known.
