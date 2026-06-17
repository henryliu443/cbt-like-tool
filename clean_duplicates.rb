def remove_struct(filepath, struct_name)
  content = File.read(filepath)
  # Regex to match `private struct Name: View { ... }`
  # We'll use a simple state machine to count braces.
  lines = content.lines
  output = []
  in_struct = false
  brace_count = 0
  
  lines.each do |line|
    if !in_struct && line =~ /private\s+struct\s+#{struct_name}\s*:\s*View/
      in_struct = true
      brace_count = line.count('{') - line.count('}')
      next
    end
    
    if in_struct
      brace_count += line.count('{') - line.count('}')
      if brace_count <= 0
        in_struct = false
      end
      next
    end
    
    output << line
  end
  
  File.write(filepath, output.join)
end

remove_struct('CBTReframe/CBTReframeApp.swift', 'ExercisesView')
remove_struct('CBTReframe/CBTReframeApp.swift', 'MoodInsightsView')
remove_struct('CBTReframe/Views/HomeView.swift', 'StreamingResultView')

puts "Duplicates removed."
