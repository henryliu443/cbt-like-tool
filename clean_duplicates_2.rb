def remove_enum(filepath, enum_name)
  content = File.read(filepath)
  lines = content.lines
  output = []
  in_enum = false
  brace_count = 0
  
  lines.each do |line|
    if !in_enum && line =~ /enum\s+#{enum_name}\b.*\{/
      in_enum = true
      brace_count = line.count('{') - line.count('}')
      next
    end
    
    if in_enum
      brace_count += line.count('{') - line.count('}')
      if brace_count <= 0
        in_enum = false
      end
      next
    end
    
    output << line
  end
  
  File.write(filepath, output.join)
end

remove_enum('CBTReframe/Services/PromptTemplates.swift', 'CognitiveDistortion')
puts "Duplicate enum removed."
