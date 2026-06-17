# Adaptive UI Analysis Report

**Summary:** The codebase contains an anti-pattern `AdaptiveScaling.kt` intended to mechanically scale dimensions, which goes against the user's directive. Additionally, there are rigid width constraints inside flex containers that should use weights, and single-column tablet bandaids that should use `WindowSizeClass` instead.

## 1. Observation
- **Mechanical Scaling Anti-pattern:** `app/src/main/java/com/henryliu/cbtreframe/ui/AdaptiveScaling.kt` defines `adaptiveDp()` and `adaptiveSp()` which blindly multiply numerical values by a screen scale factor (`widthDp / 360f`).
- **Rigid Flex Children:** In `app/src/main/java/com/henryliu/cbtreframe/android/ui/HomeView.kt` (lines 656-691), `TemplateCol` uses a hardcoded `Modifier.width(82.dp)`. It is placed inside a `Row` with `horizontalArrangement = Arrangement.SpaceEvenly`.
- **Band-aid Tablet Constraints:** In `app/src/main/java/com/henryliu/cbtreframe/ui/HomeScreen.kt` (line 382), `LoadingStep` uses `Modifier.widthIn(max = 400.dp)` to prevent the loading UI from stretching indefinitely on wide screens.
- **Valid Semantic Spacings:** Most paddings (e.g., `padding(16.dp)`), spacers (`Spacer(Modifier.height(8.dp))`), button heights (`height(56.dp)`), and dividers (`width(1.dp)`) correctly reflect semantic design constraints and are not (yet) mechanically scaled.

## 2. Logic Chain
- **Rejecting `AdaptiveScaling.kt`:** Scaling every dimension linearly by screen width transforms the UI into a magnifying glass. On a tablet, touch targets and text become comically large; on compact phones, they become unreadably small. This directly violates the directive: "而非仅把硬编码数值包装成新的硬编码" (rather than just wrapping hard-coded values into new hard-coded ones).
- **Replacing Rigid Widths with Weights:** Assigning `width(82.dp)` to `TemplateCol` prevents it from growing. In multi-size devices, a `Row` containing multiple `TemplateCol`s should use `Modifier.weight(1f)` on each child so they dynamically and evenly consume available space without leaving awkward margins.
- **Resolving Layout Strategy:** Capping `widthIn(max = 400.dp)` leaves excessive whitespace on tablets. A true adaptive UI reads the device's `WindowSizeClass`. If the width is `Medium` or `Expanded`, the UI should restructure (e.g., changing a vertical `Column` into a horizontal `Row` or implementing a Master-Detail pane), rather than just limiting the width of a single column.
- **Preserving Semantics:** The standard Material Design grid and touch targets rely on absolute spacing (e.g., 16dp margins, 48dp touch targets). These act as visual anchors and must remain as absolute `dp`.

## 3. Caveats
- I did not rewrite the UI code. This analysis outlines the architectural strategy.
- While `FlowRow` could be recommended for chips like `MoodPicker` (which currently uses `LazyRow`), `LazyRow` is an acceptable UX on mobile. The implementer should evaluate if `FlowRow` behaves better structurally on tablet `Expanded` modes.
- `WindowSizeClass` logic is not currently present in the Compose files. It must be injected from the `MainActivity` layer.

## 4. Conclusion
The implementation team must execute the following structural refactor strategy:
1. **Remove Mathematical Scaling:** Delete `AdaptiveScaling.kt` and strictly forbid the use of `.adaptiveDp()` and `.adaptiveSp()`.
2. **Implement `WindowSizeClass`:** Introduce `calculateWindowSizeClass(activity)` to the app structure. Use it to conditionally render different structural layouts (Compact vs. Expanded) instead of slapping `widthIn(max = ...)` on containers.
3. **Use Layout Weights:** In `HomeView.kt`, replace `Modifier.width(82.dp)` in `TemplateCol` with `Modifier.weight(1f)` when passed to its parent `Row`. 
4. **Preserve Semantic Bounds:** Retain all standard spacing (e.g., `16.dp`, `8.dp`, `56.dp` button heights) without scaling them. 

## 5. Verification Method
- **Static Check:** Run `grep -r "adaptiveDp" app/src/main` to ensure the mechanical scaling function is eradicated.
- **Preview / Emulator Check:** Load the app on a Tablet Emulator (e.g., Pixel Tablet). 
  - Ensure the `TemplateCol` items fill horizontal space proportionately via weights.
  - Ensure paddings (like 16dp margins) remain absolute and do not appear visually bloated.
  - Verify that the layout restructures rather than just floating a 400dp column in the center.
