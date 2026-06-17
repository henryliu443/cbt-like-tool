# Scripts Experience

## Evidence Read
- Scope: matched session 019e9102-4f0d-7733-ad16-8c2179792871.
- Matched Codex sessions (exact cwd only): 019e9102-4f0d-7733-ad16-8c2179792871.
- Current repository facts discovered from the working directory.
- Existing repository files and directories that were already present before AGENTS governance scaffolding.

### Matched Session Files
- `/Users/henry/.codex/sessions/2026/06/04/rollout-2026-06-04T13-01-48-019e9102-4f0d-7733-ad16-8c2179792871.jsonl`

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
### Session `019e9102-4f0d-7733-ad16-8c2179792871`
- User: 请评估本地工作区中的 Swift 代码库（位于 /Users/henry/cbt-like-tool/CBTReframe），并批判这个针对 Android 端的 History 迁移计划（当前计划内容位于 /Users/henry/.gemini/antigravity/brain/4ac2ec85-28df-4217-8363-d5db93116612/implementation_plan.md）。注意，我们目前就只是做迁移，没有
- Assistant: Let me start by reading the migration plan and exploring the Swift codebase in parallel.
- Assistant: Now let me read the key Swift source files relevant to History migration:
- Assistant: Now let me read additional relevant files to complete the comparison:
- Assistant: Let me also check the existing KMP shared module and Android code for comparison:
- Assistant: Now let me read the existing KMP shared and Android files:

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
