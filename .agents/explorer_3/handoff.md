# Handoff Report: Adaptive UI Refactor

## Observation
1. **Target Device Specs:** The Samsung Galaxy S26 (unreleased, but expected to iterate on the S24/S25 baseline) features an approximate 6.2-inch FHD+ display, resolution of 1080 x 2340 pixels, 19.5:9 aspect ratio, and a density of ~416 PPI (density bucket `~2.6`). This translates to a baseline screen width of approximately **412dp to 415dp**.
2. **Current Usage:** Files in `com.henryliu.cbtreframe.ui` and `com.henryliu.cbtreframe.android.ui` make extensive use of hardcoded `.dp` and `.sp` literals (e.g., `16.dp`, `20.sp`, `(-12).dp`, `4.dp.toPx()`).
3. **Context Limitations:** Some of these `.dp` usages exist within non-composable scopes, such as `Canvas { val strokeWidth = 4.dp.toPx() }`. An `@Composable` property for `.adaptiveDp` would cause compilation errors in these specific instances.

## Logic Chain
1. To accommodate all existing usages transparently without refactoring the surrounding syntax (like `Canvas` blocks or non-composable modifier pipelines), the scaling factor should rely on the system-level display metrics rather than Compose's `LocalConfiguration`.
2. Utilizing `Resources.getSystem().displayMetrics` is highly reliable for app-wide UI width scaling and avoids `@Composable` context contamination.
3. We can create an `AdaptiveScaling` singleton and map extension properties `val Number.adaptiveDp: Dp` and `val Number.adaptiveSp: TextUnit` directly against the global metrics.
4. With these defined, replacing `.dp` with `.adaptiveDp` acts as a perfect drop-in replacement — it yields a `Dp` object, maintaining compatibility with chaining methods like `.toPx()`.
5. A simple shell script using `sed` and `awk` can inject the required imports and replace the literals via Regex without risking malformed Kotlin syntax. 

## Caveats
- Using global `Resources.getSystem()` implies the scaling will not automatically respond to certain localized Compose configurations (like multi-window splits) as dynamically as `LocalConfiguration.current` would. However, for a generic CBT app scaling mechanism that must quickly convert hardcoded values, it is the safest and most robust path.
- A hard scaling factor might make UI elements disproportionately large on tablets, so the factor is capped using `.coerceIn(0.85f, 1.15f)`.

## Conclusion
We propose creating `AdaptiveScaling.kt` as an app-wide scaling utility, and utilizing an automated `bash` script with `awk/sed` to systematically replace all size hardcoding. The prototype files have been generated at `/Users/henry/cbt-like-tool/.agents/explorer_3/`.

### 1. AdaptiveScaling.kt (`/Users/henry/cbt-like-tool/.agents/explorer_3/AdaptiveScaling.kt`)
Should be placed in `app/src/main/java/com/henryliu/cbtreframe/ui/AdaptiveScaling.kt`:
```kotlin
package com.henryliu.cbtreframe.ui

import android.content.res.Resources
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AdaptiveScaling {
    const val BASE_WIDTH_DP = 412f

    val scaleFactor: Float
        get() {
            val metrics = Resources.getSystem().displayMetrics
            val screenWidthDp = metrics.widthPixels / metrics.density
            return (screenWidthDp / BASE_WIDTH_DP).coerceIn(0.85f, 1.15f)
        }
}

val Number.adaptiveDp: Dp
    get() = (this.toFloat() * AdaptiveScaling.scaleFactor).dp

val Number.adaptiveSp: TextUnit
    get() = (this.toFloat() * AdaptiveScaling.scaleFactor).sp
```

### 2. Refactor Script (`/Users/henry/cbt-like-tool/.agents/explorer_3/refactor.sh`)
This script safely injects imports and refactors literal `.dp` / `.sp` definitions:
```bash
#!/bin/bash

UI_DIRS=("./app/src/main/java/com/henryliu/cbtreframe/ui" "./app/src/main/java/com/henryliu/cbtreframe/android/ui")

for DIR in "${UI_DIRS[@]}"; do
    find "$DIR" -type f -name "*.kt" | while read -r file; do
        
        # Skip the AdaptiveScaling.kt file itself
        if [[ "$file" == *"AdaptiveScaling.kt"* ]]; then
            continue
        fi

        # 1. Inject imports right after the first import declaration
        awk '/^import/ && !done { 
            print "import com.henryliu.cbtreframe.ui.adaptiveDp"
            print "import com.henryliu.cbtreframe.ui.adaptiveSp"
            done=1 
        } 1' "$file" > temp && mv temp "$file"
        
        # 2. Replace positive literals (e.g. 16.dp -> 16.adaptiveDp, 1.5.dp -> 1.5.adaptiveDp)
        sed -i '' -E 's/([0-9]+(\.[0-9]+)?)\.dp/\1.adaptiveDp/g' "$file"
        sed -i '' -E 's/([0-9]+(\.[0-9]+)?)\.sp/\1.adaptiveSp/g' "$file"

        # 3. Replace parenthesized negative literals (e.g. (-12).dp -> (-12).adaptiveDp)
        sed -i '' -E 's/\((-?[0-9]+(\.[0-9]+)?)\)\.dp/(\1).adaptiveDp/g' "$file"
        sed -i '' -E 's/\((-?[0-9]+(\.[0-9]+)?)\)\.sp/(\1).adaptiveSp/g' "$file"

    done
done
```

## Verification Method
1. The Implementer should copy `AdaptiveScaling.kt` to the correct package location.
2. The Implementer runs `bash /Users/henry/cbt-like-tool/.agents/explorer_3/refactor.sh` from the project root.
3. Verify changes using `git diff` to ensure regex modifications correctly applied `.adaptiveDp`/`.adaptiveSp` without breaking layout syntax.
4. Run `./gradlew build` or IDE sync to verify there are no compilation errors (specifically targeting `Canvas` invocations and negative literal offsets).
