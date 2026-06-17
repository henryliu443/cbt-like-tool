require 'xcodeproj'

project_path = 'CBTReframe.xcodeproj'
project = Xcodeproj::Project.open(project_path)
target = project.targets.first

target.build_configurations.each do |config|
  config.build_settings['INFOPLIST_FILE'] = 'CBTReframe/CBTReframe-Info.plist'
end

project.save
