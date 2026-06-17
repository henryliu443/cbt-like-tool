require 'xcodeproj'

project_path = 'CBTReframe.xcodeproj'
project = Xcodeproj::Project.open(project_path)
target = project.targets.first

# Update Build Settings
target.build_configurations.each do |config|
  config.build_settings['GENERATE_INFOPLIST_FILE'] = 'NO'
  config.build_settings['INFOPLIST_FILE'] = 'CBTReframe/Info.plist'
end

project.build_configurations.each do |config|
  config.build_settings['ENABLE_USER_SCRIPT_SANDBOXING'] = 'NO'
end

project.save
puts "Successfully patched Info.plist and Sandboxing."
