#!/bin/bash

UI_DIRS=("./app/src/main/java/com/henryliu/cbtreframe/ui" "./app/src/main/java/com/henryliu/cbtreframe/android/ui")

for DIR in "${UI_DIRS[@]}"; do
    find "$DIR" -type f -name "*.kt" | while read -r file; do
        
        # Skip the AdaptiveScaling.kt file itself if it's already there
        if [[ "$file" == *"AdaptiveScaling.kt"* ]]; then
            continue
        fi

        # 1. Inject imports right after the first import declaration (using awk)
        awk '/^import/ && !done { 
            print "import com.henryliu.cbtreframe.ui.adaptiveDp"
            print "import com.henryliu.cbtreframe.ui.adaptiveSp"
            done=1 
        } 1' "$file" > temp && mv temp "$file"
        
        # 2. Replace positive/unsigned literals: 16.dp -> 16.adaptiveDp, 1.5.dp -> 1.5.adaptiveDp
        sed -i '' -E 's/([0-9]+(\.[0-9]+)?)\.dp/\1.adaptiveDp/g' "$file"
        sed -i '' -E 's/([0-9]+(\.[0-9]+)?)\.sp/\1.adaptiveSp/g' "$file"

        # 3. Replace parenthesized negative literals: (-12).dp -> (-12).adaptiveDp
        sed -i '' -E 's/\((-?[0-9]+(\.[0-9]+)?)\)\.dp/(\1).adaptiveDp/g' "$file"
        sed -i '' -E 's/\((-?[0-9]+(\.[0-9]+)?)\)\.sp/(\1).adaptiveSp/g' "$file"

    done
done
