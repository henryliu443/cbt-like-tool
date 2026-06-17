# Install Configuration

## Skill Install Path
- Install the skill folder into the target agent skill directory before use.
- When replacing an installed skill, first move the old skill to the sibling `skill_backups/<skill-name>-YYYYMMDD-HHMMSS/` folder.
- `v1.0.0` and later do not support evolution templates; replacement installs should remove any legacy `assets/templates/evolution/` content from the destination skill.

## Codex Adapter
- Keep `SKILL.md`, `agents/openai.yaml`, `references/`, `scripts/`, and `assets/` together.

## Claude Adapter
- Use `CLAUDE.md` compatibility shims only when requested; preserve existing non-managed files.

## OpenClaw Adapter
- Treat OpenClaw as an external adapter target and record project-specific setup here when confirmed.

## Compatibility Shims
- Create shims with the bundled compatibility script after AGENTS.md generation when requested.
