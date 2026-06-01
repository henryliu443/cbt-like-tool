# CBTReframe — 跨平台认知行为治疗辅助工具

一款基于认知行为治疗（CBT）理论的 AI 辅助思维重构工具。支持 iOS、iPadOS 和 Android，通过 [Skip.tools](https://skip.dev) 实现单一 Swift 代码库、双端原生渲染。

<div align="center">

**iOS** (SwiftUI + 毛玻璃) | **Android** (Jetpack Compose + Material 3)

</div>

## 核心特性

### 功能
- **AI 思维重构** — 输入负面想法，AI 帮识别认知扭曲、提供替代想法、建议行动
- **多 AI 服务支持** — OpenAI / Anthropic / DeepSeek / Gemini / Moonshot，自由切换
- **三种思维模板** — CBT 标准重构、苏格拉底提问、行为激活
- **分析深度可调** — 快速 / 平衡 / 深度
- **回应风格可选** — 简洁 / 教练式 / 温暖支持

### 体验
- **原生平台设计** — iOS 毛玻璃/液态玻璃/彩虹动效；Android Material 3/Material You
- 深色模式、时间问候语、心情标签选择器
- 自动保存所有分析、按日期分组、收藏洞察、本周回顾统计
- 首次启动 3 步引导、Face ID 保护（iOS）、生物认证（Android）

### 安全隐私
- API Key 存储于 iOS Keychain / Android EncryptedSharedPreferences（绝不上传）
- 数据仅保存在本地设备（SwiftData / Room）
- 危机关键词检测，自动显示紧急求助热线

## 系统要求

| 平台 | 版本 |
|------|------|
| **iOS** | 17.0+ |
| **Android** | API 28+ (Android 9.0) |
| **开发环境** | Xcode 15.0+ / Android Studio 2024+ |
| **语言** | Swift 5.0+ / Kotlin（Skip 自动生成） |

## 快速开始

### iOS 开发

```bash
# 克隆项目
git clone https://github.com/henryliu443/cbt-like-tool-2.git
cd cbt-like-tool-2

# 用 Xcode 打开
open CBTReframe.xcodeproj

# 选择 iOS 目标，点击运行（无第三方依赖，开箱即用）
```

### Android 开发（需要 Skip 环境）

```bash
# 1. 安装 Skip CLI
brew install skiptools/skip/skip

# 2. 验证环境
skip checkup

# 3. 构建 Android 版本
skip build

# 4. 在 Android 模拟器/真机上运行
skip run
```

**⚠️ 注意**：Android 端目前处于 Phase 4（Android 视觉打磨），集成验证还在进行中。具体状态见 `docs/progress/latest.md`。

## 项目结构

```
cbt-like-tool-2/
├── README.md                        # 本文件
├── AGENTS.md                        # Agent 交接手册（重要！）
├── implementation_plan.md           # 完整跨平台迁移计划
├── Package.swift                    # Skip 项目配置
├── CBTReframe/                      # 主源代码（Skip 编译）
│   ├── Models/
│   │   ├── AnalysisResult.swift     # 分析结果
│   │   ├── AIProvider.swift         # AI 服务商枚举
│   │   ├── ReframeMode.swift        # 分析模式/风格
│   │   └── HistoryEntry.swift       # SwiftData 数据模型
│   ├── Services/                    # AI 服务层（34 个文件，跨平台共用）
│   │   ├── OpenAIService.swift
│   │   ├── AnthropicService.swift
│   │   ├── GeminiService.swift
│   │   ├── DeepSeekService.swift
│   │   ├── MoonshotService.swift
│   │   ├── PromptTemplates.swift    # 系统提示词
│   │   └── ...其他服务
│   ├── Services/Platform/           # 平台适配层（#if SKIP 隔离）
│   │   ├── KeychainManager.swift    # iOS Keychain / Android Encrypted Pref
│   │   ├── LocalAuthManager.swift   # iOS Face ID / Android BiometricPrompt
│   │   ├── HapticManager.swift      # iOS 触感 / Android 振动
│   │   └── ReminderService.swift    # iOS 推送 / Android AlarmManager
│   ├── ViewModels/
│   │   ├── ReframeViewModel.swift
│   │   ├── SettingsViewModel.swift
│   │   └── HistoryViewModel.swift
│   ├── Views/
│   │   ├── HomeView.swift           # 首页（输入+结果）
│   │   ├── SettingsView.swift       # 设置
│   │   ├── HistoryView.swift        # 历史记录
│   │   ├── MoodInsightsView.swift   # 心情统计
│   │   └── Components/
│   │       ├── AppleIntelligenceStyle.swift  # iOS 毛玻璃/Android M3
│   │       ├── ThoughtInputCard.swift
│   │       ├── MoodTagPicker.swift
│   │       └── SafetyBannerView.swift
│   └── Assets.xcassets/             # 图片资源（深色模式）
├── Sources/                         # Skip 生成的 Swift 共享源（编译中间产物）
├── Android/                         # Android 工程文件（Skip 自动生成）
├── Darwin/                          # macOS 工程文件（Skip 自动生成）
└── docs/
    └── progress/                    # 进度快照（每个工作周期保存一份）
        ├── 2026-06-01_1744_snapshot.md
        └── ...历史快照
```

## 技术栈与架构

### 跨平台框架：Skip.tools
- **iOS 端**：编译为 100% 原生 SwiftUI，保留毛玻璃/液态玻璃/彩虹动效
- **Android 端**：Skip 在编译时将 SwiftUI 翻译为 Jetpack Compose，渲染 Material 3 组件

### 通用层（跨平台共享，一行不改）
| 层 | 技术 | 文件 |
|-----|-----|------|
| **AI 服务** | URLSession + Codable | `Services/*.swift`（34 个） |
| **Prompt 引擎** | 纯 Swift 字符串处理 | `PromptTemplates.swift` |
| **数据模型** | `struct + Codable` | `Models/*.swift` |
| **业务逻辑** | MVVM + `@Observable` | `ViewModels/*.swift` |

### 平台适配层（`#if SKIP` 隔离）
| 模块 | iOS | Android |
|------|-----|---------|
| **数据库** | SwiftData | Room（Repository 接口） |
| **密钥存储** | iOS Keychain | EncryptedSharedPreferences |
| **生物认证** | LocalAuthentication | BiometricPrompt |
| **推送通知** | UNUserNotificationCenter | WorkManager + NotificationCompat |
| **触感反馈** | UIImpactFeedbackGenerator | VibrationEffect |
| **UI 风格** | 毛玻璃/彩虹动效 | Material 3 + Material You |

## 当前开发状态

### 迁移阶段进度
```
✅ 阶段 0：环境准备
✅ 阶段 1：Repository 重构（消除 @Query 宏，引入数据库抽象层）
✅ 阶段 2：Skip 项目初始化与代码迁移
✅ 阶段 3：平台适配层（Keychain / 生物认证 / 推送 / 触感）
✅ 阶段 4：Android 视觉打磨（Material 3 降级适配、TimelineView 静态化）
🔄 阶段 5：集成验证（iOS/Android 模拟器与真机测试）
```

**最新进度**：Phase 4 完成，`swift build` 编译通过。Android 端 M3 降级与彩虹动效静态化已实现。当前在集成验证阶段。

详见 `docs/progress/latest.md` 和 `AGENTS.md`。

## 注意事项

### 开发约定
- **主分支**：`main` 保持 iOS 可运行状态
- **迁移分支**：`feature/skip-migration` 进行跨平台工作
- 每个阶段完成后在 `docs/progress/` 保存快照，命名 `YYYY-MM-DD_HHmm_snapshot.md`

### 已知坑位
1. **Skip AST 解析脆弱性**：避免在 ViewBuilder 修饰符链中间直接插入 `#if` 宏，应提取为外部 helper function
2. **TimelineView 支持缺陷**：Skip 对 TimelineView 的支持有严重问题，Android 端已降级为静态彩色，iOS 保留原效果
3. **`@Query` 宏不支持**：Skip 不支持 SwiftData 的 `@Query` 在 Android 端，已全部迁移到 ViewModel + Repository 模式
4. **macOS 编译目标**：本项目支持 iOS/macOS，使用 `#if os(iOS)` 而非 `#if !SKIP` 来隔离纯 iOS API

### 如何贡献

1. 阅读 `AGENTS.md`（所有开发者的必读文档）
2. 确认当前所在阶段，见 `docs/progress/latest.md`
3. 阅读 `implementation_plan.md` 了解完整战略
4. 开发完成后在 `docs/progress/` 保存进度快照

## 免责声明

本应用仅作为自助认知练习工具，**不能替代专业心理咨询或治疗**。如果你正处于心理危机中，请立即联系专业帮助：

- 全国心理援助热线：**400-161-9995**
- 北京心理危机研究与干预中心：**010-82951332**
- 生命热线：**400-821-1215**

## License

Apache-2.0

---

**最后更新**：2026-06-01  
**项目状态**：Phase 4 完成，Phase 5 进行中  
**联系方式**：Henry（独立开发者）
