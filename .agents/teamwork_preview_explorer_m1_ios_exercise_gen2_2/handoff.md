# Handoff Report: iOS Native Exercise Interface Redesign (Fix VETO feedback)

## Observation
During read-only investigation of the `CBTReframe` codebase, I verified the following 5 issues raised in the VETO feedback:

1. **Coroutine Leak**: `ExerciseSessionView.swift` calls `viewModel.state.collect(collector:)` inside `startObserving()`, triggered by `.onAppear`. There is no logic to cancel this asynchronous task in `.onDisappear`, nor is it wrapped in a structured concurrency construct.
2. **SwiftUI Anti-Pattern**: In `ExerciseSessionView.swift`, the custom `init(definition:)` constructs `ExerciseSessionViewModel(definition:)` eagerly on line 59, and then passes it into `_session = StateObject(wrappedValue:)`.
3. **Brittle Localization**: `FluidBreathingRenderer.swift` accepts `phase: String` and performs string literal comparisons (`if phase == "呼气"`, `if phase == "屏息"`).
4. **Hardcoded KMP Path**: `CBTReframe.xcodeproj/project.pbxproj` statically references `shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework` in its file references and `FRAMEWORK_SEARCH_PATHS`.
5. **Widget Extension**: `project.pbxproj` includes `ExerciseLiveActivity.swift` in the `CBTReframe` main app target's compile sources phase. It lacks a dedicated App Extension target required for Live Activities.

## Logic Chain

**Issue 1: Coroutine Leak**
Because Kotlin suspend functions export to Swift as `async throws` methods, invoking them directly with completion handlers or unmanaged tasks detaches them from the SwiftUI lifecycle. By switching to SwiftUI's `.task` modifier, the task implicitly binds to the view's lifetime and is automatically cancelled on view disappearance, propagating the cancellation to the Kotlin flow.

**Issue 2: SwiftUI Anti-Pattern**
`StateObject` utilizes an `@autoclosure` for its `wrappedValue` to delay instantiation until SwiftUI explicitly requests the state. Because `vm` is instantiated *before* the `StateObject` init, a new instance is created every time the View struct is initialized (which occurs frequently during parent recomposition). Moving the instantiation into the autoclosure fixes this memory/object leak.

**Issue 3: Brittle Localization**
Hardcoded UI strings decouple the rendering logic from the shared Kotlin source of truth (`ExercisePhase`). Since KMP natively exports `ExercisePhase` into Swift, relying directly on the exported enum guarantees type safety and robustness against localization changes.

**Issue 4: Hardcoded KMP Path**
A static path to `iosSimulatorArm64` inherently breaks physical iOS device builds (`iosArm64`). The KMP Gradle plugin offers the `embedAndSignAppleFrameworkForXcode` task, which reads Xcode's `$PLATFORM_NAME` and `$CONFIGURATION` environment variables during the build to dynamically compile the correct architecture framework.

**Issue 5: Widget Extension**
ActivityKit components run out-of-process via WidgetKit. They cannot be hosted in the main app target. To programmatically satisfy this requirement without manual Xcode IDE manipulation, the standard `xcodeproj` Ruby gem can manipulate the `.pbxproj` file to create the target, assign the source files, and embed the extension.

## Caveats
- No caveats regarding code paths as standard KMP mapping rules apply.
- The `xcodeproj` ruby gem is assumed to be executable in the environment. If not, the script can be adapted to Python (`Xcodeproj` module) or a Fastlane lane.
- Exact exported Swift case names for `ExercisePhase` may vary slightly depending on Kotlin version (e.g., lowercase `.inhale` vs uppercase `.INHALE`), but standard mapping is lower camel case.

## Conclusion & Fix Strategy

1. **Critical Coroutine Leak**: Refactor `ObservableExerciseSession` to expose an async `func observeState() async throws`. In `ExerciseSessionView.swift`, remove `session.startObserving()` from `.onAppear` and replace it with a `.task { try? await session.observeState() }` modifier on the main view.
2. **SwiftUI Anti-Pattern**: Rewrite the view initializer to instantiate the view model inside the autoclosure: `_session = StateObject(wrappedValue: ObservableExerciseSession(viewModel: ExerciseSessionViewModel(definition: definition)))`.
3. **Brittle Localization**: Update `FluidBreathingRenderer` to accept `var phase: ExercisePhase`. Replace string conditionals with enum case checks (e.g., `if phase == .exhale`).
4. **Hardcoded KMP Path**: Remove the hardcoded `shared.framework` from Xcode. Add a Run Script phase: `cd "$SRCROOT/../" && ./gradlew :shared:embedAndSignAppleFrameworkForXcode`. Set the `FRAMEWORK_SEARCH_PATHS` in Build Settings to `$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`.
5. **Widget Extension**: Execute a Ruby script using the `xcodeproj` gem to programmatically create an `app_extension` target. The script should:
   - Create a target named `ExerciseWidgetExtension`.
   - Remove `ExerciseLiveActivity.swift` from the main target and add it to the extension target's source phase.
   - Set `INFOPLIST_KEY_NSExtensionPointIdentifier` to `com.apple.widgetkit-extension`.
   - Add an Embed Foundation Extensions copy-files phase to the main target to embed the widget.

## Verification Method
- **Issue 1 & 2**: Run the app and use Xcode Instruments (Allocations/Leaks) or simple print statements in `deinit` to verify `ExerciseSessionViewModel` doesn't leak on view dismissal or recompose.
- **Issue 3**: Inspect `FluidBreathingRenderer.swift` to ensure no raw string literals ("呼气") exist for phase comparisons.
- **Issue 4**: Build the Xcode project for both an iOS Simulator and 'Any iOS Device'. Both must compile successfully.
- **Issue 5**: Check Xcode target list for the new Widget Extension and ensure `ExerciseLiveActivity.swift` only belongs to the extension target's compiled sources.
