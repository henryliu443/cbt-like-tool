import SwiftUI

struct FollowUpChatView: View {
    let initialThought: String
    let initialResult: AnalysisResult
    @State private var messages: [String] = []
    @State private var draft: String = ""

    var body: some View {
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
        .safeAreaInset(edge: .bottom) {
            HStack {
                TextField("继续追问...", text: $draft)
                    .textFieldStyle(.roundedBorder)
                Button("发送") {
                    let t = draft.trimmingCharacters(in: .whitespacesAndNewlines)
                    guard !t.isEmpty else { return }
                    messages.append(t)
                    draft = ""
                }
            }
            .padding()
            .background(.ultraThinMaterial)
        }
        .navigationTitle("继续探索")
    }
}
