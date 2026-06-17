# Handoff Report: Adaptive UI Strategy

**Core Findings:** Samsung Galaxy S26 base model specifications yield a display logical width of roughly 415dp. A static `Resources.getSystem().displayMetrics` approach allows computing adaptive sizing independently of `@Composable` scopes, enabling safe replacement across the entire codebase including `Canvas` DrawScopes.

## 1. Observation
- The base standard for Galaxy S26 specifications involves: 6.2" screen, 1080 x 2340 pixels resolution, ~416 PPI. This leads to a density multiplier of `2.6` (xxhdpi) and an approximate screen width of `415dp`.
- A regex search reveals ubiquitous use of `.dp` and `.sp` integer/float extensions (e.g., `16.dp`, `8.dp`, `13.sp`) in both `@Composable` UI building components and non-composable contexts (e.g., `DrawScope` in `HomeScreen.kt:996` with `4.dp.toPx()`).
- No `.px` hardcodings matching standard UI number syntax were found via `grep`.

## 2. Logic Chain
- **Hardware Specs**: Using a generic `360f` or `412f` base screen width is common in Android UI. The Galaxy S26's ~415dp logical width signifies we should define a baseline (e.g., `360f` standard Android baseline) and dynamically scale values using the ratio `actualWidthDp / 360f`.
- **Framework Constraint**: Applying `LocalConfiguration.current` to derive the screen width requires a `@Composable` context. As observed in `HomeScreen.kt`, dimensions are occasionally declared globally or within `Canvas` DrawScopes. Thus, a traditional `@Composable` extension property would fail to compile for these use-cases.
- **Solution Engine**: The framework must access the system `Resources.getSystem().displayMetrics` globally to compute the ratio without Composable requirements, allowing `Number.adaptiveDp()` to be a drop-in replacement across both Contexts and DrawScopes.
- **Migration Strategy**: Given the uniformity of syntax (`[number].dp` and `[number].sp`), `sed` regex can precisely capture these invocations while safely ignoring imports and non-number prefixes. 

## 3. Caveats
- Relying on `Resources.getSystem()` correctly fetches the absolute screen dimensions, but may not adjust correctly if the app strictly requires multi-window dynamic resizing on tablets without recomposition triggers. However, for a CBT phone app heavily rooted in S-series dimensions, this provides a highly robust immediate refactor.
- Unused imports (e.g., `androidx.compose.ui.unit.dp`) will remain post-refactor, which Android Studio's 'Optimize Imports' will need to sweep eventually.
- Files in the `android/ui` package will require explicit imports for the new `adaptiveDp` and `adaptiveSp` utilities, which the proposed `sed` script manages.

## 4. Conclusion
We must implement a framework in `app/src/main/java/com/henryliu/cbtreframe/ui/AdaptiveScaling.kt`:

```kotlin
package com.henryliu.cbtreframe.ui

import android.content.res.Resources
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AdaptiveScaling {
    const val BASE_SCREEN_WIDTH_DP = 360f // Standard Android Base

    val scale: Float
        get() {
            val metrics = Resources.getSystem().displayMetrics
            val widthDp = metrics.widthPixels / metrics.density
            return widthDp / BASE_SCREEN_WIDTH_DP
        }
}

fun Number.adaptiveDp(): Dp = (this.toFloat() * AdaptiveScaling.scale).dp
fun Number.adaptiveSp(): TextUnit = (this.toFloat() * AdaptiveScaling.scale).sp
```

**Proposed Regex Migration Script (for Terminal):**
```bash
# 1. Replace hardcoded numerical dp and sp calls
find ./app/src/main/java/com/henryliu/cbtreframe/ui ./app/src/main/java/com/henryliu/cbtreframe/android/ui -type f -name "*.kt" -exec sed -i '' -E 's/([0-9]+(\.[0-9]+)?)\.dp/\1.adaptiveDp()/g; s/([0-9]+(\.[0-9]+)?)\.sp/\1.adaptiveSp()/g' {} +

# 2. Add imports to android/ui components for the new UI extensions
find ./app/src/main/java/com/henryliu/cbtreframe/android/ui -type f -name "*.kt" -exec sed -i '' '/^package/a\
\
import com.henryliu.cbtreframe.ui.adaptiveDp\
import com.henryliu.cbtreframe.ui.adaptiveSp\
' {} +
```

## 5. Verification Method
- **Framework validation**: Build the new `AdaptiveScaling.kt` module.
- **Script validation**: Run the provided `sed` bash scripts in the project root.
- **Build execution**: Execute `./gradlew assembleDebug` to verify that `adaptiveDp()` inside `@Composable`, global variables, and `DrawScope` contexts all compile cleanly without scope errors.
