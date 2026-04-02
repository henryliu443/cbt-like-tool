import SwiftUI

struct AnalysisResult {
    let distortion: String
    let alternative: String
    let action: String
}

func analyzeThought(_ input: String) -> AnalysisResult {
    let trimmed = input.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !trimmed.isEmpty else {
        return AnalysisResult(
            distortion: "无法分析",
            alternative: "请先输入一个想法",
            action: "试着写下你的感受"
        )
    }

    let pool: [AnalysisResult] = [
        AnalysisResult(
            distortion: "灾难化思维",
            alternative: "事情不一定会变得最糟，过去类似的情况我也度过了",
            action: "先喝一口水，暂停一下"
        ),
        AnalysisResult(
            distortion: "非黑即白思维",
            alternative: "大多数事情都存在中间地带，不是全对或全错",
            action: "写下这件事中一个还不错的部分"
        ),
        AnalysisResult(
            distortion: "过度概括",
            alternative: "一次失败不代表永远失败，每次经历都不同",
            action: "回忆一次你成功应对类似情况的经历"
        ),
        AnalysisResult(
            distortion: "读心术",
            alternative: "我无法确定别人在想什么，也许他们根本没注意到",
            action: "下次直接友善地问对方怎么想"
        ),
        AnalysisResult(
            distortion: "情绪化推理",
            alternative: "感觉糟糕不等于事实糟糕，情绪会过去的",
            action: "做三次深呼吸，感受一下身体的变化"
        ),
    ]

    let index = abs(trimmed.hashValue) % pool.count
    return pool[index]
}

struct ContentView: View {
    @State private var inputText = ""
    @State private var result: AnalysisResult?

    var body: some View {
        ZStack {
            Color(.systemGroupedBackground)
                .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 24) {
                    Text("CBT 思维重构")
                        .font(.title.bold())
                        .padding(.top, 40)

                    VStack(alignment: .leading, spacing: 12) {
                        Text("写下你的负面想法")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)

                        TextEditor(text: $inputText)
                            .frame(minHeight: 100)
                            .padding(8)
                            .background(Color(.systemBackground))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(Color(.separator), lineWidth: 1)
                            )
                    }
                    .padding(.horizontal)

                    Button {
                        withAnimation(.easeInOut(duration: 0.3)) {
                            result = analyzeThought(inputText)
                        }
                    } label: {
                        Text("Analyze")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(Color.accentColor)
                            .foregroundStyle(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .padding(.horizontal)

                    if let result {
                        VStack(alignment: .leading, spacing: 16) {
                            resultRow(label: "认知扭曲", value: result.distortion)
                            Divider()
                            resultRow(label: "替代想法", value: result.alternative)
                            Divider()
                            resultRow(label: "小行动", value: result.action)
                        }
                        .padding(20)
                        .background(Color(.systemBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                        .shadow(color: .black.opacity(0.06), radius: 8, y: 4)
                        .padding(.horizontal)
                        .transition(.opacity.combined(with: .move(edge: .bottom)))
                    }

                    Spacer(minLength: 40)
                }
            }
        }
    }

    private func resultRow(label: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.body)
        }
    }
}

#Preview {
    ContentView()
}
