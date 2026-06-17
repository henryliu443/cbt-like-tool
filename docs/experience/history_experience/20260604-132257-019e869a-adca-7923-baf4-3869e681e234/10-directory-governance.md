# Directory Governance Experience

## Evidence Read
- Scope: matched session 019e869a-adca-7923-baf4-3869e681e234.
- Matched Codex sessions (exact cwd only): 019e869a-adca-7923-baf4-3869e681e234.
- Current repository facts discovered from the working directory.
- Existing repository files and directories that were already present before AGENTS governance scaffolding.

### Matched Session Files
- `/Users/henry/.codex/sessions/2026/06/02/rollout-2026-06-02T12-32-24-019e869a-adca-7923-baf4-3869e681e234.jsonl`

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
### Session `019e869a-adca-7923-baf4-3869e681e234`
- User: Hi Codex. You were 100% right. I failed to read the codebase deeply before proposing. The user has indeed set up `CBTApplication.kt` with Koin, `MainActivity.kt` with the Compose UI, and `AIServiceImpl.kt` with a mock st
- Assistant: Let me dive into the codebase to understand the existing structure before validating this step.
- Assistant: Now let me check how Koin wires everything, and how the ViewModel calls AIService:
- Assistant: Now let me check the Koin DI to understand if `SettingsManager` is wired properly and what platform modules exist:
- Assistant: Now let me check the build.gradle files to see Ktor version and dependencies available:
- Assistant: Now I have a thorough understanding of the codebase. Here's my validation/critique. **I agree — this is the correct next step.** The mock `flow { ... delay(100) }` is the placeholder, and replacing it with real Ktor SSE 

## Task Context
- The current working directory already contained project content but did not yet contain a root `AGENTS.md`. This bootstrap experience records how the existing code and the exact-cwd Codex session history should be read together before the workspace is normalized.
- This `Directory Governance` record is generated from session evidence plus landed repository content so future agents can understand what work was underway, what files mattered, and what governance repairs were required.

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
- `Directory Governance` bootstrap records should capture both the current repository evidence and the matching Codex session excerpts so later agents can understand not only what files exist, but why they exist and what restructuring or governance work was already being discussed.
- The bootstrap process should remain conservative: exact-cwd matching only, no silent directory normalization, and no fabricated narrative beyond what the file tree and the matched session excerpts support.
- User-visible paths should stay normalized and readable. Repository-relative evidence such as `src/main.py`, `docs/experience/1-workflow.md`, or `engineering/demo-app/` is easier to audit than collapsed raw path strings.

## Next Application
- Use this `Directory Governance` template when the next missing-AGENTS bootstrap needs to recover context from local Codex sessions and landed files before ordinary docs governance begins.
- If exact-cwd session evidence is missing, say that clearly and lean harder on file evidence instead of pretending the missing conversation details are known.
