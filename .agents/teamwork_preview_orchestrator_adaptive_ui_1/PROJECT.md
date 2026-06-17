# Project: Adaptive UI Refactor

## Architecture
- Module/package boundaries: `./app/src/main/java/com/henryliu/cbtreframe/ui/` and `./app/src/main/java/com/henryliu/cbtreframe/android/ui/`
- Prioritize layout strategy (WindowSizeClass, weights, adaptive containers) over mechanical dp/sp replacement.
- Preserve standard spacing/padding values that represent intentional design boundaries.

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | Structural UI Strategy | Analyze codebase and propose structural changes (WindowSizeClass, weights) | none | IN_PROGRESS |
| 2 | Refactor Structural Layouts | Apply weights and adaptive containers to UI components | 1 | PLANNED |

## Interface Contracts
- Use Compose built-in adaptive utilities: `fillMaxWidth`, `weight`, `WindowWidthSizeClass`.
