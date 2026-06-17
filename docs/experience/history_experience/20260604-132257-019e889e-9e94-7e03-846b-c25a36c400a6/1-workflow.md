# Workflow Experience

## Evidence Read
- Scope: matched session 019e889e-9e94-7e03-846b-c25a36c400a6.
- Matched Codex sessions (exact cwd only): 019e889e-9e94-7e03-846b-c25a36c400a6.
- Current repository facts discovered from the working directory.
- Existing repository files and directories that were already present before AGENTS governance scaffolding.

### Matched Session Files
- `/Users/henry/.codex/sessions/2026/06/02/rollout-2026-06-02T21-55-57-019e889e-9e94-7e03-846b-c25a36c400a6.jsonl`

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
### Session `019e889e-9e94-7e03-846b-c25a36c400a6`
- User: Create `AIServiceImpl.kt` in `shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/services/`. It should implement Ktor SSE network parsing for the DeepSeek API. Do not use Ktor SSE plugins, but directly use `body
- Assistant: Let me start by exploring the project structure to understand the codebase conventions and the Alpha MVP plan.
- Assistant: No `services/` directory exists yet. Let me examine the existing service implementations and the key protocol/interface files.
- Assistant: Now let me also check the OpenAIService for comparison, and look for any existing SSE parsing patterns.
- Assistant: Now let me check for the KeychainProvider and existing AIServiceImpl reference (it already exists but in the root shared directory, not in services/).
- Assistant: Good, now I have a clear picture. Let me also check for any AGENTS.md files.

## Task Context
- The current working directory already contained project content but did not yet contain a root `AGENTS.md`. This bootstrap experience records how the existing code and the exact-cwd Codex session history should be read together before the workspace is normalized.
- This `Workflow` record is generated from session evidence plus landed repository content so future agents can understand what work was underway, what files mattered, and what governance repairs were required.

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
- 完整流程链: detect missing root AGENTS.md -> confirm the workspace already has landed content -> read exact-cwd Codex sessions -> extract user and assistant intent from those sessions -> inspect the current file tree -> ask whether to normalize structure when the layout violates the contract -> migrate legacy docs paths -> generate history experience snapshots per matched session -> synthesize the latest current experience set -> continue with AGENTS generation and verification.
- 完整逻辑链: the working directory is the identity boundary; the exact cwd selects the session transcripts; the transcripts explain why current files exist; the current files confirm what was actually landed; the structure gate decides whether reorganization must be confirmed; the docs bootstrap turns those inputs into governed memory under `docs/experience/` and `docs/experience/history_experience/`.
- 闭环: if exact-cwd sessions are found, archive them as historical experience first; if no exact-cwd sessions are found, mark the conversation context as missing and generate the latest experience from landed files only; if structure is invalid, stop and ask before reorganizing; after normalization, regenerate current experience so the governed state reflects the repaired repository.
- For path handling, always render workspace evidence as normalized repository-relative paths such as `src/main.py` or absolute filesystem paths with real separators. Never collapse them into unreadable joined strings.

```mermaid
flowchart TD
    A[Missing root AGENTS.md detected] --> B[Check whether workspace already has landed content]
    B --> C[Match exact-cwd Codex sessions]
    C --> D[Inspect current files and directories]
    D --> E[Ask whether to normalize structure when contract violations exist]
    E --> F[Migrate legacy docs paths into governed docs layout]
    F --> G[Write per-session history_experience snapshots]
    G --> H[Write latest current docs/experience files]
    H --> I[Continue AGENTS generation and verification]
```

## Next Application
- Reuse this bootstrap only for workspaces that already contain real content but still lack a root `AGENTS.md`.
- Keep the exact-cwd rule strict so that neighboring repositories never leak into the current experience set.
- After the initial bootstrap, switch back to the normal handoff and experience cadence instead of regenerating history on every run.
