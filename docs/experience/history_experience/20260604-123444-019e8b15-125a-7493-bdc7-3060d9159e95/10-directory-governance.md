# Directory Governance Experience

## Evidence Read
- Scope: matched session 019e8b15-125a-7493-bdc7-3060d9159e95.
- Matched Codex sessions (exact cwd only): 019e8b15-125a-7493-bdc7-3060d9159e95.
- Current repository facts discovered from the working directory.
- Existing repository files and directories that were already present before AGENTS governance scaffolding.

### Matched Session Files
- `/Users/henry/.codex/sessions/2026/06/03/rollout-2026-06-03T09-24-34-019e8b15-125a-7493-bdc7-3060d9159e95.jsonl`

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
### Session `019e8b15-125a-7493-bdc7-3060d9159e95`
- User: 本次任务以 HomeView.swift 为唯一设计标准（Source of Truth）。Android 端目标是复刻，而不是优化、改良或重新设计。请先完整理解 SwiftUI 的信息架构，再进行任何实现。 最重要的是遵循 HomeView.swift 的组件层级和用户流程顺序： headerSection → todayDashboard → newAnalysisButton（仅结果页显示）→ 主流程区 → 更多选项 → quic
- Assistant: Let me start by finding and reading `HomeView.swift` to fully understand the component hierarchy before touching any Android code.
- Assistant: Found both files. Let me read the SwiftUI source first — the ground truth — then I'll look at the existing Android version.
- Assistant: The existing Android HomeView.kt is only 36 lines — a stub. Let me now read both files in full, plus any related components.
- Assistant: Now let me read the supporting Android components and understand the project structure.
- Assistant: Let me now read the critical supporting files — the ViewModel, components, and existing patterns.

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
