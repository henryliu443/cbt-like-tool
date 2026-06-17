# Directory Manager

This file is the strict gate for creating, moving, renaming, or deleting local project folders and remote deployment workspace folders.

## Required Review
- Read this file before changing folder structure.
- Run `python <codex-home>/skills/agents-md-generator/scripts/manage_dirs.py review <project> --input change.json` before directory changes.
- Do not move, rename, or delete governance folders without explicit user force-confirmation.
- If review blocks a change, refuse default execution and explain the risk to the user.
- If the user explicitly force-confirms a blocked change, archive old dir manager content under `history_dir_manager/YYYYMMDD-HHMMSS/` before changing structure.

## Blocked By Default
- Paths outside the project, absolute paths, parent traversal, wildcards, or shell-unsafe path characters.
- New top-level folders not listed in `planned_structure.json`.
- Workspace engineering config files such as `project.local.json`, `project.remote.json`, or `server_list.local.json` outside `.settings/`.
- Any remote attempt to copy `.settings/*.local.json` such as `.settings/server_list.local.json` into the remote workspace.
- Remote deployment folders not listed in `planned_structure.json` remote_deployment planning.
- Moving or deleting `.agents/`, `docs/dir_manager/`, `docs/handoff/`, or `docs/git_manager/`.
- Moving source, tests, docs, dist, scripts, assets, references, or agents folders to unplanned locations.
- Mixing generated output, release packages, or temporary references into source folders.

## User Force Override
- Explain why the request is unreasonable or risky.
- State severe hazards such as broken tests, invalid release packages, stale AGENTS.md scopes, broken history links, or failed skill installation.
- Ask the user to explicitly confirm forced directory structure modification.
- Run `python <codex-home>/skills/agents-md-generator/scripts/manage_dirs.py archive <project> --reason force-confirmed directory override` before applying a force-confirmed folder change.
- Record confirmation and risk in the next handoff.
