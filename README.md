# CBT 思维重构 (CBTReframe)

一款基于认知行为治疗（CBT）理论的 AI 辅助思维重构工具。当你陷入负面想法时，它帮你识别认知扭曲、找到更平衡的视角、迈出下一步行动。

## 功能特性

### 核心功能
- **AI 思维重构** — 输入负面想法，AI 帮你识别认知扭曲类型、提供替代想法、建议具体行动
- **多服务商支持** — OpenAI / Anthropic / DeepSeek / 离线本地模式，自由切换
- **三种思维模板** — CBT 标准重构、苏格拉底提问、行为激活
- **分析深度可调** — 快速（1-2句）/ 平衡（标准）/ 深度（详细分析）
- **回应风格可选** — 简洁 / 教练式 / 温暖支持

### 界面与体验
- 现代卡片式 UI，支持深色模式
- 时间问候语 + 每日一句鼓励
- 心情标签选择器
- 渐变按钮 + 弹性动画 + 触感反馈
- 3 步引导页（首次启动）
- iPhone + iPad 适配

### 历史与记录
- 所有分析自动保存
- 按日期分组浏览
- 收藏有帮助的洞察
- 本周回顾统计

### 安全与隐私
- API Key 存储于 iOS Keychain，不上传任何服务器
- 数据仅保存在设备本地（SwiftData）
- Face ID 保护历史记录（可选）
- 一键清除所有数据
- 危机关键词检测，自动展示紧急求助热线

## 支持的 AI 模型

| 服务商 | 可用模型 |
|--------|----------|
| OpenAI | GPT-4.1, GPT-4.1 Mini, GPT-4.1 Nano, GPT-4o, GPT-4o Mini |
| Anthropic | Claude Sonnet 4, Claude 3.5 Haiku |
| DeepSeek | DeepSeek Chat, DeepSeek Reasoner |
| 本地 | 内置离线分析（无需 API Key） |

## 系统要求

- iOS 17.0+
- Xcode 15.0+
- Swift 5.0+

## 快速开始

1. **克隆项目**
   ```bash
   git clone https://github.com/henryliu443/cbt-like-tool.git
   cd cbt-like-tool
   ```

2. **用 Xcode 打开**
   ```bash
   open CBTReframe.xcodeproj
   ```

3. **选择目标设备，点击运行**（无第三方依赖，开箱即用）

4. **首次启动** — 跟随引导页选择 AI 服务商，填入 API Key（或直接选择"本地模式"离线使用）

## 项目结构

```
CBTReframe/
├── CBTReframeApp.swift              # 入口：SwiftData 容器 + 引导页/主页路由
├── ContentView.swift                # 保留的占位 View
├── Models/
│   ├── AnalysisResult.swift         # 分析结果模型（Codable）
│   ├── AIProvider.swift             # AI 服务商枚举 + 模型列表
│   ├── ReframeMode.swift            # 分析模式 / 回应风格 / 思维模板枚举
│   └── HistoryEntry.swift           # SwiftData 历史记录模型
├── Services/
│   ├── AIServiceProtocol.swift      # 统一协议 + 工厂
│   ├── OpenAIService.swift          # OpenAI API 实现
│   ├── AnthropicService.swift       # Anthropic API 实现
│   ├── DeepSeekService.swift        # DeepSeek API 实现
│   ├── LocalAnalysisService.swift   # 离线本地分析
│   ├── KeychainManager.swift        # Keychain 安全存储
│   └── PromptTemplates.swift        # 系统提示词 + 危机检测
├── ViewModels/
│   ├── ReframeViewModel.swift       # 首页逻辑
│   ├── SettingsViewModel.swift      # 设置管理
│   └── HistoryViewModel.swift       # 历史记录逻辑
├── Views/
│   ├── HomeView.swift               # 首页（输入 + 结果）
│   ├── SettingsView.swift           # 设置页
│   ├── HistoryView.swift            # 历史记录页
│   ├── ResultCardView.swift         # 结果卡片组件
│   ├── OnboardingView.swift         # 引导页
│   └── Components/
│       ├── ThoughtInputCard.swift   # 输入框卡片
│       ├── MoodTagPicker.swift      # 心情标签
│       └── SafetyBannerView.swift   # 危机求助横幅
└── Assets.xcassets/                 # 图标 + 颜色资源（深色模式适配）
```

## 技术栈

- **UI**: SwiftUI
- **架构**: MVVM（`@Observable`）
- **持久化**: SwiftData
- **网络**: URLSession（无第三方依赖）
- **安全**: iOS Keychain（Security framework）
- **最低版本**: iOS 17

## 免责声明

本应用仅作为自助认知练习工具，**不能替代专业心理咨询或治疗**。如果你正处于心理危机中，请立即联系专业帮助：

- 全国心理援助热线：**400-161-9995**
- 北京心理危机研究与干预中心：**010-82951332**
- 生命热线：**400-821-1215**

## License

MIT
