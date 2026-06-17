# Project: Bug Fixes and UI Alignment

## Architecture
- `FollowUpChatView.kt`: Android UI component requiring exception handling.
- `AIService.kt` / `AIServiceImpl.kt`: Shared Kotlin module dead code to be removed.
- `HomeView.kt` / `ResultCardView.kt`: Android UI components requiring Design Token standardization.

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | M1: Fixes | R1, R2, R3 from ORIGINAL_REQUEST.md | none | DONE |

## Interface Contracts
- None added.

## Code Layout
- Android UI: `app/src/main/java/com/henryliu/cbtreframe/android/ui/`
- Shared logic: `shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/`
