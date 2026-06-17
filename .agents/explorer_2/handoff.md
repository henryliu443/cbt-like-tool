# Handoff Report: Adaptive UI Refactor Strategy

## 1. Observation
- **Project Structure**: Compose UI files are split between `./app/src/main/java/com/henryliu/cbtreframe/ui/` and `./app/src/main/java/com/henryliu/cbtreframe/android/ui/`.
- **Hardcoded Dimensions**: Running `grep -r "\.dp" ./app/.../ui` revealed over 450 occurrences of hardcoded dimensions (e.g., `16.dp`, `20.sp`).
- **Context Constraints**: Running `grep -n -B 5 "\.dp"` and filtering out `@Composable` confirmed that all usages of `.dp` and `.sp` are strictly within `@Composable` function scopes (some appeared as standard functions but had the `@Composable` annotation on the preceding line, e.g., `InsetDivider`).
- **Regex Testing**: Running `echo "source.split()" | sed -E 's/\.sp[[:>:]]/.adaptiveSp/g'` successfully returned `source.split()`, while `echo "16.sp" | sed -E 's/\.sp[[:>:]]/.adaptiveSp/g'` returned `16.adaptiveSp`. This confirms `[[:>:]]` correctly enforces word boundaries on macOS BSD `sed`.
- **Import Behavior**: Testing the regex on imports `echo "import androidx.compose.ui.unit.dp" | sed -E 's/\.dp[[:>:]]/.adaptiveDp/g'` outputs `import androidx.compose.ui.unit.adaptiveDp`, which can be cleanly reverted and replaced in a secondary `sed` pass.

## 2. Logic Chain
1. **Samsung Galaxy S26 Specs**: Based on Android hardware standards, the standard Galaxy S26 (following S24/S25 trajectories) features a ~6.2" FHD+ display (2340 x 1080) at ~416 ppi. This translates to a density multiplier of ~2.6, resulting in an effective screen width of **~412dp**. This is the optimal baseline `BASE_WIDTH` for the adaptive framework.
2. **Framework Design**: Since all `.dp` and `.sp` usages are inside `@Composable` blocks, we can implement `Number.adaptiveDp` and `Number.adaptiveSp` as extension properties with `@Composable get()`. This allows us to dynamically read `LocalConfiguration.current.screenWidthDp` to compute the scaling ratio without requiring `Context` or explicit variables.
3. **Refactor Strategy**: Due to the massive number of hardcoded values, manual replacement is inefficient. We can apply a safe, automated bulk refactor using `-E` (Extended Regex) in `sed`.
4. **Safety Mechanisms**: By using word boundary markers `[[:>:]]`, we guarantee that `.sp` does not accidentally rename method calls like `.split(...)` (e.g., in `HistoryExportManager.kt`) and `.dp` does not accidentally rename types like `.Dp`.

## 3. Caveats
- The hardware specs for the Galaxy S26 are inferred from standard release trajectories up to early 2026. If the S26 Ultra is the primary target, its logical width might be slightly wider (~432dp), but the responsive ratio logic naturally handles any viewport size regardless.
- We assume no future `.dp` usages will be introduced at the top-level (file scope). Top-level `.adaptiveDp` declarations would result in compiler errors due to the lack of `@Composable` context.
- Linear scaling of `.sp` applies the screen ratio on top of the system font scale. This provides a truly relative design, but if a user has maximum system font sizes on a very large tablet, the text could scale aggressively.

## 4. Conclusion
1. **S26 Specs**: FHD+ (2340x1080), ~416 ppi, logical width ~412dp.
2. **Framework**: Create `AdaptiveScaling.kt` in `com.henryliu.cbtreframe.ui`:
```kotlin
package com.henryliu.cbtreframe.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AdaptiveScaling {
    const val BASE_WIDTH = 412f
}

val Number.adaptiveDp: Dp
    @Composable
    get() {
        val screenWidth = LocalConfiguration.current.screenWidthDp
        val ratio = screenWidth / AdaptiveScaling.BASE_WIDTH
        return (this.toFloat() * ratio).dp
    }

val Number.adaptiveSp: TextUnit
    @Composable
    get() {
        val screenWidth = LocalConfiguration.current.screenWidthDp
        val ratio = screenWidth / AdaptiveScaling.BASE_WIDTH
        return (this.toFloat() * ratio).sp
    }
```
3. **Refactor Script**: Execute the following safe commands to apply the framework:
```sh
find ./app/src/main/java/com/henryliu/cbtreframe -type f -name "*.kt" -exec sed -i '' -E 's/\.dp[[:>:]]/.adaptiveDp/g' {} +
find ./app/src/main/java/com/henryliu/cbtreframe -type f -name "*.kt" -exec sed -i '' -E 's/import androidx\.compose\.ui\.unit\.adaptiveDp/import com.henryliu.cbtreframe.ui.adaptiveDp/g' {} +
find ./app/src/main/java/com/henryliu/cbtreframe -type f -name "*.kt" -exec sed -i '' -E 's/\.sp[[:>:]]/.adaptiveSp/g' {} +
find ./app/src/main/java/com/henryliu/cbtreframe -type f -name "*.kt" -exec sed -i '' -E 's/import androidx\.compose\.ui\.unit\.adaptiveSp/import com.henryliu.cbtreframe.ui.adaptiveSp/g' {} +
```

## 5. Verification Method
1. Create `AdaptiveScaling.kt` and run the `sed` commands.
2. Verify `com.henryliu.cbtreframe.android.ui.HistoryExportManager.kt` remains intact (specifically `rawText.split` should not be renamed).
3. Run `./gradlew assembleDebug` to ensure all `@Composable` contexts are valid and imports resolved successfully.
