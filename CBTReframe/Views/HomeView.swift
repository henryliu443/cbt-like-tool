import SwiftUI
import SwiftData

struct HomeView: View {
    @Environment(\.modelContext) private var modelContext
    @Bindable var viewModel: ReframeViewModel
    @State private var selectedMood: String = ""
    @State private var isButtonPressed = false
    @FocusState private var isInputFocused: Bool

    var body: some View {
        ZStack {
            Color(.systemGroupedBackground)
                .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 20) {
                    headerSection
                    ThoughtInputCard(text: $viewModel.inputText, isFocused: $isInputFocused)
                    templatePicker
                    MoodTagPicker(selectedMood: $selectedMood)
                    analyzeButton

                    if viewModel.showCrisisBanner {
                        SafetyBannerView()
                            .transition(.asymmetric(
                                insertion: .opacity.combined(with: .scale(scale: 0.95)),
                                removal: .opacity
                            ))
                    }

                    if let errorMessage = viewModel.errorMessage {
                        errorBanner(errorMessage)
                    }

                    if let result = viewModel.result {
                        ResultCardView(result: result)
                            .transition(.asymmetric(
                                insertion: .opacity
                                    .combined(with: .move(edge: .bottom))
                                    .combined(with: .scale(scale: 0.95)),
                                removal: .opacity
                            ))
                    }

                    Spacer(minLength: 60)
                }
                .padding(.top, 16)
            }
            .scrollDismissesKeyboard(.interactively)
        }
        .onTapGesture {
            isInputFocused = false
        }
    }

    private var headerSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(viewModel.greeting)
                        .font(.largeTitle.bold())
                        .foregroundStyle(Color("TextPrimary"))

                    Text(viewModel.todayQuote)
                        .font(.subheadline)
                        .foregroundStyle(Color("TextSecondary"))
                        .lineLimit(2)
                }

                Spacer()

                Image(systemName: "brain.head.profile")
                    .font(.system(size: 36))
                    .foregroundStyle(
                        LinearGradient(
                            colors: [Color("GradientStart"), Color("GradientEnd")],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
            }
        }
        .padding(.horizontal)
        .padding(.top, 8)
    }

    private var templatePicker: some View {
        TemplatePickerView(
            selectedTemplate: Binding(
                get: { viewModel.activeTemplate },
                set: { viewModel.quickTemplate = $0 }
            ),
            suggestedTemplate: viewModel.suggestedTemplate
        )
    }

    private var analyzeButton: some View {
        Button {
            isInputFocused = false
            let impactFeedback = UIImpactFeedbackGenerator(style: .medium)
            impactFeedback.impactOccurred()

            Task {
                await viewModel.analyzeThought(modelContext: modelContext)
                if viewModel.result != nil {
                    let notificationFeedback = UINotificationFeedbackGenerator()
                    notificationFeedback.notificationOccurred(.success)
                }
            }
        } label: {
            HStack(spacing: 8) {
                if viewModel.isLoading {
                    ProgressView()
                        .tint(.white)
                } else {
                    Image(systemName: "arrow.triangle.2.circlepath")
                        .font(.headline)
                }
                Text(viewModel.isLoading ? "正在分析..." : viewModel.activeTemplate.shortLabel)
                    .font(.headline)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(
                LinearGradient(
                    colors: [Color("GradientStart"), Color("GradientEnd")],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .foregroundStyle(.white)
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
        .disabled(viewModel.inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || viewModel.isLoading)
        .opacity(viewModel.inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? 0.6 : 1)
        .scaleEffect(isButtonPressed ? 0.96 : 1)
        .onLongPressGesture(minimumDuration: .infinity, pressing: { pressing in
            withAnimation(.easeInOut(duration: 0.15)) {
                isButtonPressed = pressing
            }
        }, perform: {})
        .padding(.horizontal)
    }

    private func errorBanner(_ message: String) -> some View {
        HStack(spacing: 10) {
            Image(systemName: "exclamationmark.circle")
                .foregroundStyle(.red)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(Color("TextPrimary"))
            Spacer()
            Button {
                withAnimation { viewModel.errorMessage = nil }
            } label: {
                Image(systemName: "xmark.circle.fill")
                    .foregroundStyle(Color("TextSecondary"))
            }
        }
        .padding(14)
        .background(Color.red.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal)
        .transition(.opacity.combined(with: .move(edge: .top)))
    }
}
