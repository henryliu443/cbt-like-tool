#!/bin/bash

# Configuration
PROJECT="Darwin/CBTReframe.xcodeproj"
SCHEME="CBTReframe App"
DESTINATION="platform=iOS Simulator,name=iPhone 15 Pro"

echo "Running XCUITest for $SCHEME..."

# Create a temporary directory for xcresult
XCRESULT_DIR=$(mktemp -d -t xcresult)
XCRESULT_PATH="$XCRESULT_DIR/TestResult.xcresult"

# Run xcodebuild test
xcodebuild test -project "$PROJECT" -scheme "$SCHEME" -destination "$DESTINATION" -resultBundlePath "$XCRESULT_PATH" | xcpretty
EXIT_CODE=${PIPESTATUS[0]}

if [ $EXIT_CODE -eq 0 ]; then
    echo -e "\033[32mSuccess: All UI tests passed.\033[0m"
else
    echo -e "\033[31mTests Failed with exit code $EXIT_CODE.\033[0m"
    echo "Checking for crash logs..."
    
    # Check if xcresult exists
    if [ -d "$XCRESULT_PATH" ]; then
        echo "Extracting crash info from xcresult..."
        # Extract crash logs if any
        CRASH_LOGS=$(xcrun xcresulttool get --path "$XCRESULT_PATH" --format json | grep -i "EXC_BREAKPOINT" || true)
        if [ ! -z "$CRASH_LOGS" ]; then
            echo -e "\033[31mCrash found (EXC_BREAKPOINT):\033[0m"
            echo "$CRASH_LOGS"
        else
            echo "No explicit EXC_BREAKPOINT found in xcresult JSON summary."
        fi
        
        # Look in diagnostic directories inside xcresult
        find "$XCRESULT_PATH" -type f -name "*.ips" -o -name "*.crash" | while read -r crash_file; do
            echo -e "\033[31mCrash file: $crash_file\033[0m"
            grep -C 5 "EXC_BREAKPOINT" "$crash_file" || true
            echo "---"
        done
    else
        echo "No TestResult.xcresult generated."
    fi
    
    echo -e "\033[33mNote: If xcodebuild failed immediately with 'not currently configured for the test action', you must manually add the CBTReframeUITests target to the Xcode project and check its Test action in the 'CBTReframe App' scheme.\033[0m"
fi

exit $EXIT_CODE
