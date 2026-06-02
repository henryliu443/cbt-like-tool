import SwiftUI

struct ExercisesView: View {
    private let items: [(String, String, String)] = [
        ("4-7-8 呼吸", "wind", "吸气4秒，屏息7秒，呼气8秒，重复4轮。"),
        ("渐进式肌肉放松", "figure.cooldown", "从脚到头逐段紧张3秒并放松。"),
        ("5-4-3-2-1 感官着陆", "hand.raised", "说出看到5样、触到4样、听到3样、闻到2样、尝到1样。"),
        ("身体扫描", "figure.mind.and.body", "从头到脚觉察紧绷与放松部位。"),
    ]

    var body: some View {
        NavigationStack {
            List {
                ForEach(items, id: \.0) { item in
                    ExerciseRowView(item: item)
                }
            }
            .navigationTitle("练习")
        }
    }
}

struct ExerciseRowView: View {
    let item: (String, String, String)
    @State private var isExpanded = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button {
                withAnimation { isExpanded.toggle() }
            } label: {
                HStack {
                    Label(item.0, systemImage: item.1)
                        .font(.headline)
                        .foregroundStyle(.primary)
                    Spacer()
                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .foregroundStyle(.secondary)
                        .font(.caption)
                }
            }
            .buttonStyle(.plain)
            
            if isExpanded {
                Text(item.2)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .transition(.opacity)
            }
        }
        .padding(.vertical, 6)
    }
}
