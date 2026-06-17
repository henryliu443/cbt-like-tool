# dev-0.4.0 implementation plan

## 1. Complete the Dynamic Model Refactor & Onboarding
- `[x]` Intercept network failures in Onboarding.
- `[x]` Add a Model Confirmation UI step upon successful sync during Onboarding.
- `[x]` Implement Cold Start Background Sync in `SettingsManager` or the main activity.
- `[x]` Implement the Model Invalidation Banner (non-blocking) in the main UI.

## 2. History View Feature Parity (Android)
- `[x]` Add Metadata Tags to History Cards.
- `[x]` Implement Favorites UI.
- `[x]` Wire up Export Flow.
- `[x]` Add Weekly Review Card stats.
- `[x]` Implement Swipe-to-delete and Context Menus.
- `[x]` Enable detail expansion via `ResultCardView`.
- `[x]` Create proper Empty State UI.
