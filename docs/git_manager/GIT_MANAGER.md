# Git Manager

## Workspace Management
- Keep current development work in the working folder unless the user requests a separate worktree.
- Do not repoint repositories with `git config core.worktree`; use normal checkout/merge or explicit `git worktree` commands instead.

## Branch Configuration
- Protected branches: `master`, `release`.
- Development branches are allowed as temporary local work branches.
- Before releasing an installable `dist/` package, commit all work and merge development branches into `master`.
- Use `python <codex-home>/skills/agents-md-generator/scripts/manage_docs.py release-prepare <project> --version vX.Y.Z --skill-dir skills/<skill-name>` to auto-commit governed paths from the active temporary branch, merge it into `master`, and delete the local branch before packaging.
- If a branch has unmerged commits, merge it to `master` before cleanup; never discard it silently.
- After release preparation, delete local branches other than `master` and `release`.
- Do not delete remote branches unless the user explicitly requests remote cleanup.
- Run `python <codex-home>/skills/agents-md-generator/scripts/manage_docs.py release-gate <project> --version vX.Y.Z --skill-dir skills/<skill-name> --phase pre|post` before and after packaging to verify branch, worktree, release artifact, release receipt, and parity gates.

## Release Configuration
- Place installable releases under `dist/`.
- Name installable release folders as `<name>-vx.x.x` and create a matching zip when required.
- Build installable releases with `python <codex-home>/skills/agents-md-generator/scripts/manage_docs.py package-release <project> --version vX.Y.Z --skill-dir skills/<skill-name>` so the versioned release directory, matching zip, and `RELEASE_RECEIPT.json` provenance stay aligned.
- Different-version release directories and zip files are immutable history by default; do not delete, overwrite, or rewrite them during a new packaging run.
- Rebuilding the same version may replace only the current target release directory and its matching zip; no other `dist/` artifact may change.
- Installable `dist/` release copies for skill development must be sanitized before packaging; replace sensitive values in the dist copy only and use typed placeholders such as `<REDACTED_API_KEY>`, `<REDACTED_PASSWORD>`, `<REDACTED_EMAIL>`, and `<REDACTED_LOCAL_PATH>`.
- The release receipt must record sanitized files, placeholder types, and post-sanitization hashes; undeclared or unfinished sanitization blocks installation.
- Install only from the versioned release directory after receipt validation; never install directly from the source skill folder.
- Package only after branch cleanup and release records are complete.
- The release commit must include the release artifacts and the current `docs/git_manager/CHANGELOG.md` entry.
- If the release is for a skill project and the user did not explicitly say whether to install after release, release handling must ask the install question instead of silently stopping. Engineering projects must not ask to install a skill.

## Change Log
- Update `docs/git_manager/CHANGELOG.md` before each commit that changes governed release or git-management behavior.
- Archive the previous `CHANGELOG.md` to `docs/git_manager/history_git_manager/YYYYMMDD-HHMMSS/CHANGELOG.md` before writing the next current entry.
- Use `python <codex-home>/skills/agents-md-generator/scripts/manage_docs.py git-changelog <project> --input changelog.json` to rotate and write the current change summary.

## Current Version
- Record the active version here during release preparation and keep detailed changes in `CHANGELOG.md`.
