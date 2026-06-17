require 'xcodeproj'

project_path = 'CBTReframe.xcodeproj'
project = Xcodeproj::Project.open(project_path)
target = project.targets.first

# Add Run Script Build Phase for KMP
kmp_script = "cd \"$SRCROOT\"\n./gradlew :shared:embedAndSignAppleFrameworkForXcode"
phase = target.new_shell_script_build_phase("Build KMP Framework")
phase.shell_script = kmp_script

# Move the phase before "Compile Sources" (which is usually index 0 or 1 after dependencies)
target.build_phases.delete(phase)
target.build_phases.insert(0, phase)

# Update Build Settings
target.build_configurations.each do |config|
  search_paths = config.build_settings['FRAMEWORK_SEARCH_PATHS'] || ['$(inherited)']
  search_paths = [search_paths] if search_paths.is_a?(String)
  search_paths << '"$(SRCROOT)/shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)"'
  config.build_settings['FRAMEWORK_SEARCH_PATHS'] = search_paths.uniq

  ldflags = config.build_settings['OTHER_LDFLAGS'] || ['$(inherited)']
  ldflags = [ldflags] if ldflags.is_a?(String)
  ldflags << '-lsqlite3'
  ldflags << '-framework'
  ldflags << 'shared'
  config.build_settings['OTHER_LDFLAGS'] = ldflags.uniq
end

project.save
puts "Successfully configured KMP linking in CBTReframe.xcodeproj"
