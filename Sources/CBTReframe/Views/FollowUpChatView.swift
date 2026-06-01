import SwiftUI

struct FollowUpChatView: View {
    let initialThought: String
    let initialResult: AnalysisResult
    @State private var messages: [String] = []
    @State private var draft: String = ""

    var body: some View {
        VStack(spacing: 0) {
            List {
                Section("原始想法") {
                    Text(initialThought)
                }
                Section("上一轮结论") {
                    Text(initialResult.alternative)
                }
                Section("继续探索") {
                    ForEach(messages, id: \.self) { msg in
                        Text(msg)
                    }
                }
            }
            
            HStack {
                TextField("继续追问...", text: $draft)
                    .textFieldStyle(.roundedBorder)
                Button("发送") {
                    let t = draft.trimmingCharacters(in: CharacterSet.whitespacesAndNewlines)
                    guard !t.isEmpty else { return }
                    messages.append(t)
                    draft = ""
                }
            }
            .padding()
            #if !SKIP
            .background(.ultraThinMaterial)
            #else
            .background(Color("CardBackground").opacity(0.95))
            #endif
        }
        .navigationTitle("继续探索")
    }
}
