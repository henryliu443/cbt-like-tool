import SwiftUI

struct SettingsView: View {
    @Bindable var viewModel: SettingsViewModel
    @State private var showClearConfirmation = false
    @State private var showKeyField = false

    var body: some View {
        NavigationStack {
            Form {
                aiProviderSection
                if viewModel.selectedProvider.requiresAPIKey {
                    apiKeySection
                }
                modelSection
                reframeModeSection
                responseStyleSection
                promptTemplateSection
                privacySection
                aboutSection
            }
            .navigationTitle("设置")
            .alert("确认清除", isPresented: $showClearConfirmation) {
                Button("清除所有数据", role: .destructive) {
                    viewModel.clearAllData()
                }
                Button("取消", role: .cancel) {}
            } message: {
                Text("这将删除所有 API Key、历史记录和设置。此操作不可撤销。")
            }
        }
    }

    private var aiProviderSection: some View {
        Section {
            ForEach(AIProvider.allCases) { provider in
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(provider.displayName)
                            .font(.body)
                        if !provider.requiresAPIKey {
                            Text("无需 API Key，离线可用")
                                .font(.caption)
                                .foregroundStyle(Color("TextSecondary"))
                        }
                    }
                    Spacer()
                    if viewModel.selectedProvider == provider {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundStyle(Color("AccentColor"))
                    }
                }
                .contentShape(Rectangle())
                .onTapGesture {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        viewModel.selectedProvider = provider
                    }
                }
            }
        } header: {
            Label("AI 服务商", systemImage: "cpu")
        }
    }

    private var apiKeySection: some View {
        Section {
            HStack {
                if showKeyField {
                    TextField("粘贴你的 API Key", text: $viewModel.apiKeyInput)
                        .font(.system(.body, design: .monospaced))
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                } else {
                    SecureField("粘贴你的 API Key", text: $viewModel.apiKeyInput)
                        .font(.system(.body, design: .monospaced))
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }

                Button {
                    showKeyField.toggle()
                } label: {
                    Image(systemName: showKeyField ? "eye.slash" : "eye")
                        .foregroundStyle(Color("TextSecondary"))
                }
                .buttonStyle(.plain)
            }

            Button {
                viewModel.saveAPIKey()
            } label: {
                HStack {
                    Image(systemName: "key.fill")
                    Text("保存 Key")
                }
            }

            if viewModel.hasAPIKey {
                HStack(spacing: 6) {
                    Image(systemName: "checkmark.shield.fill")
                        .foregroundStyle(.green)
                        .font(.caption)
                    Text("已安全存储在 Keychain 中")
                        .font(.caption)
                        .foregroundStyle(Color("TextSecondary"))
                }
            }
        } header: {
            Label("API Key", systemImage: "key")
        } footer: {
            Text("Key 仅存储在你设备的 Keychain 中，不会上传到任何服务器。")
        }
    }

    private var modelSection: some View {
        Section {
            Picker("模型", selection: $viewModel.selectedModelId) {
                ForEach(viewModel.selectedProvider.availableModels) { model in
                    Text(model.name).tag(model.id)
                }
            }
        } header: {
            Label("模型选择", systemImage: "cube")
        }
    }

    private var reframeModeSection: some View {
        Section {
            ForEach(ReframeMode.allCases) { mode in
                HStack {
                    Image(systemName: mode.icon)
                        .frame(width: 24)
                        .foregroundStyle(Color("AccentColor"))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(mode.rawValue)
                            .font(.body)
                        Text(mode.description)
                            .font(.caption)
                            .foregroundStyle(Color("TextSecondary"))
                    }
                    Spacer()
                    if viewModel.reframeMode == mode {
                        Image(systemName: "checkmark")
                            .foregroundStyle(Color("AccentColor"))
                    }
                }
                .contentShape(Rectangle())
                .onTapGesture {
                    viewModel.reframeMode = mode
                }
            }
        } header: {
            Label("分析深度", systemImage: "slider.horizontal.3")
        }
    }

    private var responseStyleSection: some View {
        Section {
            Picker("回应风格", selection: $viewModel.responseStyle) {
                ForEach(ResponseStyle.allCases) { style in
                    Text(style.rawValue).tag(style)
                }
            }
            .pickerStyle(.segmented)

            Text(viewModel.responseStyle.description)
                .font(.caption)
                .foregroundStyle(Color("TextSecondary"))
        } header: {
            Label("回应风格", systemImage: "text.bubble")
        }
    }

    private var promptTemplateSection: some View {
        Section {
            ForEach(PromptTemplate.allCases) { tmpl in
                HStack {
                    Image(systemName: tmpl.icon)
                        .frame(width: 24)
                        .foregroundStyle(Color("AccentColor"))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(tmpl.rawValue)
                            .font(.body)
                        Text(tmpl.description)
                            .font(.caption)
                            .foregroundStyle(Color("TextSecondary"))
                    }
                    Spacer()
                    if viewModel.promptTemplate == tmpl {
                        Image(systemName: "checkmark")
                            .foregroundStyle(Color("AccentColor"))
                    }
                }
                .contentShape(Rectangle())
                .onTapGesture {
                    viewModel.promptTemplate = tmpl
                }
            }
        } header: {
            Label("思维模板", systemImage: "doc.text")
        }
    }

    private var privacySection: some View {
        Section {
            Toggle(isOn: $viewModel.useFaceID) {
                HStack(spacing: 10) {
                    Image(systemName: "faceid")
                        .foregroundStyle(Color("AccentColor"))
                    Text("Face ID 保护历史记录")
                }
            }

            Button(role: .destructive) {
                showClearConfirmation = true
            } label: {
                HStack {
                    Image(systemName: "trash")
                    Text("清除所有数据")
                }
            }
        } header: {
            Label("隐私与安全", systemImage: "lock.shield")
        }
    }

    private var aboutSection: some View {
        Section {
            HStack {
                Text("版本")
                Spacer()
                Text("1.5.0")
                    .foregroundStyle(Color("TextSecondary"))
            }
            HStack {
                Text("当前服务商")
                Spacer()
                Text(viewModel.selectedProvider.displayName)
                    .foregroundStyle(Color("TextSecondary"))
            }
            HStack {
                Text("当前模型")
                Spacer()
                Text(viewModel.selectedModel.name)
                    .foregroundStyle(Color("TextSecondary"))
            }
        } header: {
            Label("关于", systemImage: "info.circle")
        }
    }
}
