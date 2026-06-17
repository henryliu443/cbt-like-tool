# Handoff Report: iOS Native Exercise Interface Redesign (VETO Fix Strategy)

## Observation

I investigated the files related to the 5 VETO feedback items from the previous iteration:

1. **Coroutine Leak**: `ExerciseSessionView.swift` (line 27) uses `viewModel.state.collect(collector: collector) { err in ... }`. This is a non-suspending completion handler bridging a Kotlin Flow, meaning it executes in the background and is never cancelled when the view disappears.
2. **SwiftUI Anti-Pattern**: `ExerciseSessionView.swift` (line 59) initializes `let vm = ExerciseSessionViewModel(definition: definition)` before passing it into `StateObject(wrappedValue:)`. Since it's evaluated outside the `@autoclosure` of `wrappedValue:`, a new `ExerciseSessionViewModel` instance is eagerly created (and subsequently discarded) on every view recomposition.
3. **Brittle Localization**: `FluidBreathingRenderer.swift` (line 18) uses hardcoded strings: `if phase == "呼气"`, taking a `var phase: String`. `ExercisePhase.kt` exports an enum with `INHALE`, `HOLD`, `EXHALE`, `REST`.
4. **Hardcoded KMP Path**: `CBTReframe.xcodeproj/project.pbxproj` (lines 637, 679) hardcodes the `FRAMEWORK_SEARCH_PATHS` to `"$(SRCROOT)/shared/build/bin/iosSimulatorArm64/debugFramework"`, which causes the build to fail on physical devices. `shared/build.gradle.kts` does not define a custom `packForXcode` task, standard KMP capabilities are available.
5. **Widget Extension**: `ExerciseLiveActivity.swift` is part of the `CBTReframe` main app group. The `.pbxproj` does not have a separate App Extension target for WidgetKit/ActivityKit, which is structurally required by iOS for Live Activities.

## Logic Chain

1. **Fixing Issue 1 (Coroutine Leak)**: Swift 5.5+ and Kotlin 1.5.30+ support `async/await` bridging for KMP `suspend` functions. By wrapping the `.collect` call inside a Swift `Task`, we tie the execution to the Task's lifecycle. When the Task is cancelled (e.g., on view disappear), the underlying Kotlin Coroutine is also cancelled, properly releasing the background scope.
2. **Fixing Issue 2 (Anti-Pattern)**: The `@StateObject` initializer takes an `@autoclosure` for `wrappedValue`. Moving the instantiation directly inline (`_session = StateObject(wrappedValue: ObservableExerciseSession(viewModel: ExerciseSessionViewModel(definition: definition)))`) delays execution until the StateObject is actually allocated by SwiftUI, preventing eager evaluation on recomposition.
3. **Fixing Issue 3 (Localization)**: We must change `var phase: String` to `var phase: ExercisePhase` in `FluidBreathingRenderer.swift`. The string comparisons should be updated to use the KMP enum cases (e.g., `phase == .exhale` or `phase == ExercisePhase.exhale`). `ExerciseSessionView` should pass `session.state.phase` instead of `.displayName`.
4. **Fixing Issue 4 (KMP Path)**: To support both Simulator and Device, we must use the standard KMP task `embedAndSignAppleFrameworkForXcode`. This task reads Xcode environment variables and builds the framework for the active architecture, placing it in `$(SRCROOT)/shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`. We must update `FRAMEWORK_SEARCH_PATHS` to this dynamic path and add a Run Script Phase.
5. **Fixing Issue 5 (Widget Extension)**: We can use the Ruby `xcodeproj` gem to programmatically create the App Extension target, configure the `Info.plist` properties via Build Settings, add `ExerciseLiveActivity.swift`, link KMP and iOS frameworks, and embed the extension in the main app.

## Caveats

- I have not actually executed the Ruby script to modify `.pbxproj` to avoid destructive side-effects during this read-only phase. The implementing agent should verify the script's success by checking if the project opens in Xcode.
- Swift KMP enums are sometimes namespaced (e.g., `SharedExercisePhase.exhale`); the implementer must check the exact generated Objective-C header name for `ExercisePhase` if `.exhale` fails to compile.
- If `embedAndSignAppleFrameworkForXcode` task fails, it's usually due to missing environment variables. When run from an Xcode Run Script Phase, those variables (`$CONFIGURATION`, `$SDK_NAME`, `$ARCHS`) are automatically provided.

## Conclusion

The redesign strategy should be implemented by the next agent with the following exact changes:

1. **ExerciseSessionView.swift**:
   - Add `private var collectTask: Task<Void, Never>?` to `ObservableExerciseSession`.
   - Update `startObserving()` to assign the task: `collectTask = Task { try? await viewModel.state.collect(collector: collector) }`.
   - Add `func stopObserving() { collectTask?.cancel(); collectTask = nil }` and call it in `.onDisappear`.
   - Update the initializer: `_session = StateObject(wrappedValue: ObservableExerciseSession(viewModel: ExerciseSessionViewModel(definition: definition)))`.
2. **FluidBreathingRenderer.swift**:
   - `import shared` and change `phase` type to `ExercisePhase`. Update `if` conditions to use enum cases (e.g., `ExercisePhase.exhale`).
3. **KMP Xcode Integration (.pbxproj)**:
   - Change `FRAMEWORK_SEARCH_PATHS` to `"$(SRCROOT)/shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)"`.
   - Add a Run Script build phase before Compile Sources:
     ```bash
     cd "$SRCROOT"
     ./gradlew :shared:embedAndSignAppleFrameworkForXcode
     ```
4. **Widget Extension Creation**:
   - Use the provided Ruby script (in Verification Method) to create the Widget Extension target programmatically.

## Verification Method

**To Verify the Changes:**
1. **Compilation**: Run `xcodebuild -project CBTReframe.xcodeproj -scheme CBTReframe -sdk iphonesimulator build` to ensure the project compiles with the new dynamic framework path.
2. **Widget Extension**: Execute the following Ruby script (`fix_target.rb`) and verify a new `ExerciseWidgetExtension` target appears in `project.pbxproj`:
```ruby
require 'xcodeproj'
project = Xcodeproj::Project.open('CBTReframe.xcodeproj')

# 1. Create Target
target = project.new_target(:app_extension, 'ExerciseWidgetExtension', :ios, '17.0')

# 2. Build Settings (using GENERATE_INFOPLIST_FILE)
target.build_configurations.each do |config|
  config.build_settings['PRODUCT_BUNDLE_IDENTIFIER'] = 'com.henryliu.cbtreframe.ExerciseWidgetExtension'
  config.build_settings['GENERATE_INFOPLIST_FILE'] = 'YES'
  config.build_settings['CURRENT_PROJECT_VERSION'] = '1'
  config.build_settings['MARKETING_VERSION'] = '1.0'
  config.build_settings['INFOPLIST_KEY_NSExtensionPointIdentifier'] = 'com.apple.widgetkit-extension'
  config.build_settings['SKIP_INSTALL'] = 'YES'
  config.build_settings['SWIFT_VERSION'] = '5.0'
end

# 3. Add Source
file_ref = project.main_group.find_subpath('CBTReframe/LiveActivity/ExerciseLiveActivity.swift', false)
target.source_build_phase.add_file_reference(file_ref)
main_target = project.targets.find { |t| t.name == 'CBTReframe' }
main_target.source_build_phase.files_references.delete(file_ref) # Remove from main

# 4. Frameworks
%w[WidgetKit ActivityKit SwiftUI].each { |fw| target.add_system_framework(fw) }
shared_fwk = project.frameworks_group.files.find { |f| f.name == 'shared.framework' }
target.frameworks_build_phase.add_file_reference(shared_fwk) if shared_fwk

# 5. Embed in Main App
embed_phase = main_target.new_copy_files_build_phase('Embed App Extensions')
embed_phase.symbol_dst_subfolder_spec = :plug_ins
build_file = embed_phase.add_file_reference(target.product_reference)
build_file.settings = { 'ATTRIBUTES' => ['RemoveHeadersOnCopy'] }

project.save
```
3. **Leak Test**: Run the app, open the Exercise view, and put it in the background. Verify in the Xcode memory debugger or logs that the Kotlin flow collection is cancelled.
