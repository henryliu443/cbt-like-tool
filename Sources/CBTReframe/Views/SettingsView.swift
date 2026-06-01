import SwiftUI
import Darwin

struct SettingsView: View {
#if !SKIP
    @Environment(\.modelContext) private var modelContext
#endif
    @EnvironmentObject private var globalSettings: GlobalSettings
    @Bindable var viewModel: SettingsViewModel
    @State private var showClearConfirmation = false
    @State private var showKeyField = false
    @State private var showFaceIDDisableBlockedAlert = false
    @State private var showDisableDisclaimerConfirm = false

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
                reminderSection
                disclaimerSection
                aboutSection
            }
            .navigationTitle("设置")
            .toolbar {
                if viewModel.isRefreshingModels {
                    ToolbarItem(placement: .automatic) {
                        HStack(spacing: 6) {
                            ProgressView()
#if !SKIP
                                .controlSize(.small)
#endif
                            Text("获取模型中")
                                .font(.caption)
                                .foregroundStyle(Color("TextSecondary"))
                        }
                        .accessibilityLabel("正在获取模型列表")
                    }
                }
            }
            .alert("确认清除", isPresented: $showClearConfirmation) {
                Button("清除所有数据", role: .destructive) {
                    viewModel.clearAllData()
                    globalSettings.resetToDefaults()
                }
                Button("取消", role: .cancel) {}
            } message: {
                Text("这将删除所有 API Key、历史记录和设置。此操作不可撤销。")
            }
            .alert("无法关闭 Face ID 保护", isPresented: $showFaceIDDisableBlockedAlert) {
                Button("我知道了", role: .cancel) {}
            } message: {
                Text("请先通过 Face ID 验证，才能关闭历史记录保护。")
            }
            .alert("确认关闭免责声明？", isPresented: $showDisableDisclaimerConfirm) {
                Button("继续开启", role: .cancel) {
                    viewModel.hasAcceptedDisclaimer = true
                }
                Button("关闭并退出 App", role: .destructive) {
                    viewModel.hasAcceptedDisclaimer = false
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                        exit(0)
                    }
                }
            } message: {
                Text("关闭后将立即退出 App。")
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
#if !SKIP
                .contentShape(Rectangle())
#endif
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
                        #if os(iOS)
                        .textInputAutocapitalization(.never)
                        #endif
                        .autocorrectionDisabled()
                } else {
                    SecureField("粘贴你的 API Key", text: $viewModel.apiKeyInput)
                        .font(.system(.body, design: .monospaced))
                        #if os(iOS)
                        .textInputAutocapitalization(.never)
                        #endif
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
                HStack(spacing: 8) {
                    if viewModel.isSavingAPIKey {
                        ProgressView()
                    } else {
                        Image(systemName: "key.fill")
                    }
                    Text(viewModel.isSavingAPIKey ? "保存中…" : "保存 Key")
                }
            }
            .disabled(viewModel.isSavingAPIKey)

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
            VStack(alignment: .leading, spacing: 6) {
                if viewModel.isRefreshingModels && viewModel.hasAPIKey {
                    HStack(spacing: 6) {
                        ProgressView()
#if !SKIP
                            .controlSize(.small)
#endif
                        Text("正在从服务商获取可用模型…")
                            .font(.caption)
                            .foregroundStyle(Color("AccentColor"))
                    }
                }
                Text("Key 仅存储在你设备的 Keychain 中，不会上传到任何服务器。")
            }
        }
    }

    private var modelSection: some View {
        Section {
            if viewModel.isRefreshingModels {
                HStack(spacing: 10) {
                    ProgressView()
                    VStack(alignment: .leading, spacing: 2) {
                        Text("正在获取模型列表")
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(Color("TextPrimary"))
                        Text("请稍候，完成后可在此选择模型")
                            .font(.caption)
                            .foregroundStyle(Color("TextSecondary"))
                    }
                    Spacer(minLength: 0)
                }
                .padding(.vertical, 4)
#if !SKIP
                .accessibilityElement(children: .combine)
#endif
            }
            Picker("模型", selection: $viewModel.selectedModelId) {
                ForEach(viewModel.resolvedModels(for: viewModel.selectedProvider)) { model in
                    Text(model.name).tag(model.id)
                }
            }
            .disabled(viewModel.isRefreshingModels && viewModel.selectedProvider.requiresAPIKey)

            if viewModel.selectedProvider.requiresAPIKey {
                Button {
                    Task { await viewModel.refreshModels() }
                } label: {
                    Label("刷新模型列表", systemImage: "arrow.clockwise")
                }
                .disabled(viewModel.isRefreshingModels || !viewModel.hasAPIKey)
            }
        } header: {
            HStack {
                Label("模型选择", systemImage: "cube")
                if viewModel.isRefreshingModels {
                    Spacer()
                    Text("获取中…")
                        .font(.caption)
                        .foregroundStyle(Color("AccentColor"))
                }
            }
        } footer: {
            Group {
                if viewModel.isRefreshingModels {
                    Text("正在连接服务商并拉取当前账号可用的模型，完成后列表会自动更新。")
                        .font(.caption)
                } else if let err = viewModel.modelsListError {
                    Text(err)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                } else if viewModel.selectedProvider.requiresAPIKey {
                    Text("保存 API Key 后会自动从服务商拉取最新可用模型并缓存在本机；也可手动刷新。拉取失败时使用内置备选列表。")
                        .font(.caption)
                }
            }
        }
    }

    private var reframeModeSection: some View {
        Section {
            ForEach(ThinkingTemplate.AnalysisDepth.allCases) { mode in
                HStack {
                    Image(systemName: mode.icon)
                        .frame(width: 24)
                        .foregroundStyle(Color("AccentColor"))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(mode.displayName)
                            .font(.body)
                        Text(mode.description)
                            .font(.caption)
                            .foregroundStyle(Color("TextSecondary"))
                    }
                    Spacer()
                    if globalSettings.analysisDepth == mode {
                        Image(systemName: "checkmark")
                            .foregroundStyle(Color("AccentColor"))
                    }
                }
#if !SKIP
                .contentShape(Rectangle())
#endif
                .onTapGesture {
                    globalSettings.analysisDepth = mode
                }
            }
        } header: {
            Label("分析深度", systemImage: "slider.horizontal.3")
        }
    }

    private var responseStyleSection: some View {
        Section {
            Picker("回应风格", selection: $globalSettings.responseStyle) {
                ForEach(ThinkingTemplate.AppResponseStyle.allCases) { style in
                    Text(style.displayName).tag(style)
                }
            }
            .pickerStyle(.segmented)

            Text(globalSettings.responseStyle.description)
                .font(.caption)
                .foregroundStyle(Color("TextSecondary"))
        } header: {
            Label("回应风格", systemImage: "text.bubble")
        }
    }

    private var promptTemplateSection: some View {
        Section {
            ForEach(ThinkingTemplate.allCases) { tmpl in
                HStack {
                    Image(systemName: tmpl.icon)
                        .frame(width: 24)
                        .foregroundStyle(Color("AccentColor"))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(tmpl.displayName)
                            .font(.body)
                        Text(tmpl.description)
                            .font(.caption)
                            .foregroundStyle(Color("TextSecondary"))
                    }
                    Spacer()
                    if globalSettings.thinkingTemplate == tmpl {
                        Image(systemName: "checkmark")
                            .foregroundStyle(Color("AccentColor"))
                    }
                }
#if !SKIP
                .contentShape(Rectangle())
#endif
                .onTapGesture {
                    globalSettings.thinkingTemplate = tmpl
                }
            }
        } header: {
            Label("思维模板", systemImage: "doc.text")
        }
    }

    private var privacySection: some View {
        Section {
            Toggle(isOn: Binding(
                get: { viewModel.useFaceID },
                set: { newValue in
                    handleFaceIDToggleChanged(newValue)
                }
            )) {
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
                Text(AppMetadata.versionLabel)
                    .foregroundStyle(Color("TextSecondary"))
#if !SKIP
                    .font(.body.monospacedDigit())
#else
                    .font(.body)
#endif
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
        } footer: {
            Text("在「历史」可搜索与收藏记录，并导出当前列表为 JSON（V2.1）。")
                .font(.footnote)
        }
    }

    private var reminderSection: some View {
        Section {
            Toggle("每日提醒", isOn: $viewModel.dailyReminderEnabled)
            if viewModel.dailyReminderEnabled {
                DatePicker(
                    "提醒时间",
                    selection: Binding(
                        get: {
                            var comps = DateComponents()
                            comps.hour = viewModel.reminderHour
                            comps.minute = viewModel.reminderMinute
                            return Calendar.current.date(from: comps) ?? Date()
                        },
                        set: { newDate in
                            let comps = Calendar.current.dateComponents([.hour, .minute], from: newDate)
                            viewModel.reminderHour = comps.hour ?? 21
                            viewModel.reminderMinute = comps.minute ?? 0
                        }
                    ),
                    displayedComponents: .hourAndMinute
                )
            }
        } header: {
            Label("提醒", systemImage: "bell")
        } footer: {
            Text("每日提醒通过系统 Push 通知发送（本地通知）。请在系统设置里允许通知。")
                .font(.caption)
        }
    }

    private var disclaimerSection: some View {
        Section {
            NavigationLink {
                DisclaimerDetailView(isSheet: false)
            } label: {
                Label("阅读完整免责声明与服务协议", systemImage: "doc.text.magnifyingglass")
            }

            Toggle(isOn: Binding(
                get: { viewModel.hasAcceptedDisclaimer },
                set: { newValue in
                    if newValue {
                        viewModel.hasAcceptedDisclaimer = true
                    } else {
                        showDisableDisclaimerConfirm = true
                    }
                }
            )) {
                Text("我同意本应用免责声明与服务协议")
            }
            Text("若你处于危机中，请立即联系专业机构或急救服务。")
                .font(.caption)
                .foregroundStyle(Color("TextSecondary"))
        } header: {
            Label("免责声明与服务协议", systemImage: "exclamationmark.triangle")
        }
    }

    private func handleFaceIDToggleChanged(_ newValue: Bool) {
        if newValue {
            viewModel.useFaceID = true
            return
        }
        Task {
            let ok = await viewModel.authenticateWithBiometrics(reason: "关闭 Face ID 历史记录保护")
            if ok {
                viewModel.useFaceID = false
            } else {
                viewModel.useFaceID = true
                showFaceIDDisableBlockedAlert = true
            }
        }
    }
}
