# CBTReframe × Android 跨平台战略规划

## 目标与背景

你想保留 Swift/SwiftUI 的开发体验，同时让这个 App 在安卓上以完全原生的 Material 3 运行，
不使用 Web 容器，不维护两套 UI 代码，不切换到 Flutter。

**选定方案：[Skip.tools](https://skip.dev)**
- 在 iOS/Mac/iPad 上：100% 原生 SwiftUI（液态玻璃 / 毛玻璃 / 弹性动画，原样保留）
- 在 Android 上：Skip 在编译时把 SwiftUI 翻译成 Jetpack Compose，Material 3 原生渲染
- 核心逻辑（AI 请求、Prompt、解析、设置）：一行不用改，完整复用
- 平台差异（Keychain、SwiftData、生物认证）：用 `#if SKIP` 宏隔离，各自最优实现

---

## 现状诊断（已读完全部核心代码）

### ✅ 天然兼容 Skip，零改动
| 模块 | 文件 | 原因 |
|------|------|------|
| 全部 AI 服务 | `OpenAIService` `AnthropicService` `GeminiService` `DeepSeekService` `MoonshotService` | 纯 `URLSession` + `Codable`，SkipFoundation 完整支持 |
| Prompt 引擎 | `PromptTemplates` `PromptBuilder` `LLMJSONSanitizer` | 纯 Swift 字符串逻辑 |
| 全部数据模型 | `AnalysisResult` `ReframeMode` `AIProvider` `CognitiveDistortion` | 纯 `struct/enum + Codable` |
| ViewModel 逻辑 | `HistoryViewModel` `ReframeViewModel`（业务部分）| `@Observable` 在 Skip 中支持 |
| 安全检测 | `RiskRouting` `PromptTemplates` 中的危机关键词 | 纯字符串逻辑 |

### ⚠️ 需要适配（用 `#if SKIP` 隔离）
| 模块 | 文件 | 适配内容 |
|------|------|---------|
| **密钥存储** | `KeychainManager.swift` | iOS: Security framework；Android: `EncryptedSharedPreferences` |
| **数据库** | `CBTReframeApp.swift` + `SettingsViewModel` + `HistoryViewModel` 中的 `ModelContext` | iOS: SwiftData；Android: Room（通过 Repository 接口隔离） |
| **生物认证** | `SettingsViewModel.authenticateWithFaceID` | iOS: `LocalAuthentication`；Android: `BiometricPrompt` |
| **推送通知** | `ReminderService.swift` | iOS: `UNUserNotificationCenter`；Android: `AlarmManager` + `NotificationManager` |
| **触感反馈** | `HapticManager.swift` | iOS: `UIImpactFeedbackGenerator`；Android: `VibrationEffect` |
| **统计图表** | `MoodInsightsView`（`import Charts`）| iOS: Swift Charts；Android: Compose Canvas 自绘或 `Vico` 库 |
| **AppleIntelligenceStyle** | `AppleIntelligenceStyle.swift` + `TimelineView` | iOS 保留彩虹动效；Android 用 Material 3 渐变动效替代 |

### 🏗️ 需要架构重构（为 Skip 铺路）
| 问题 | 具体位置 | 解法 |
|------|----------|------|
| `@Query` 宏直接写在 View 里 | `CBTReframeApp.swift` 的 `MoodInsightsView`（有 2 个 `@Query`）| 把数据查询移到 ViewModel，引入 Repository 协议 |
| `ModelContext` 直接注入 ViewModel | `HistoryViewModel.toggleFavorite(modelContext:)` `SettingsViewModel.clearAllData(modelContext:)` | 改为 Repository 内部管理 context，ViewModel 不再接收 ModelContext |
| `UIKit` 依赖 | `SettingsViewModel` 中的 `UINotificationFeedbackGenerator` | 移入 `HapticManager`，用 `#if SKIP` 隔离 |
| `import UIKit` | `SettingsViewModel.swift` 第 4 行 | 替换为通用的 `HapticManager` |

---

## 分阶段执行计划

### 阶段 0：环境准备（半天）
> 这阶段不动任何现有代码

- [ ] 安装 Skip CLI：`brew install skiptools/skip/skip`
- [ ] 运行 `skip checkup` 验证环境（需要 Xcode + Android Studio + JDK）
- [ ] 阅读 Skip 官方文档中关于 SwiftData 和 Keychain 的适配章节
- [ ] 确认 Skip 对 `@Observable` 和 `async/await` 的支持现状

---

### 阶段 1：Repository 重构（1-2 天，先做，独立价值）
> 即使不用 Skip，这步也是好的架构实践，消除现有技术债

#### 目标
抽象出数据库访问层，ViewModel 不再直接持有 `ModelContext`

#### 具体任务

**1.1 定义 Repository 协议（新建文件 `Repositories/` 目录）**
```swift
// HistoryRepository.swift
protocol HistoryRepository {
    func fetchAll() async throws -> [HistoryEntry]
    func insert(_ entry: HistoryEntry) async throws
    func toggleFavorite(_ entry: HistoryEntry) async throws
    func deleteAll() async throws
}

// ThoughtRepository.swift
protocol ThoughtRepository {
    func fetchAll() async throws -> [ThoughtEntry]
    func insert(_ entry: ThoughtEntry) async throws
    func deleteAll() async throws
}

// MoodRepository.swift
protocol MoodRepository {
    func fetchAll() async throws -> [MoodCheckIn]
    func insert(_ checkIn: MoodCheckIn) async throws
    func deleteAll() async throws
}
```

**1.2 iOS 实现（用 SwiftData，用 `#if !SKIP` 包裹）**
```swift
// SwiftDataHistoryRepository.swift
#if !SKIP
import SwiftData
final class SwiftDataHistoryRepository: HistoryRepository {
    private let context: ModelContext
    init(context: ModelContext) { self.context = context }
    // ... SwiftData 实现
}
#endif
```

**1.3 重构 HistoryViewModel**
- 移除 `toggleFavorite(_ entry:, modelContext:)` 的 `ModelContext` 参数
- ViewModel 持有 `HistoryRepository` 协议引用，不再知道 SwiftData 的存在

**1.4 重构 SettingsViewModel**
- 移除 `clearAllData(modelContext:)` 的 `ModelContext` 参数
- 移除 `import UIKit`，把触感反馈移入 `HapticManager`

**1.5 重构 MoodInsightsView（最关键）**
- 把 `@Query` 移出 View，改为在新建的 `MoodInsightsViewModel` 中管理数据

---

### 阶段 2：Skip 项目初始化（1 天）
> 用 Skip CLI 创建新的跨平台项目结构，把阶段 1 重构好的代码迁移进去

- [ ] `skip init --appid com.cbt.reframe CBTReframe` 创建 Skip 项目
- [ ] 把现有 `Models/`、`Services/`、`ViewModels/`、`Views/` 迁移进 Skip 项目结构
- [ ] 验证 iOS 端在 Skip 项目中编译通过（此时 Android 端还不用跑通）

---

### 阶段 3：平台适配层（2-3 天）
> 用 `#if SKIP` 宏为每个平台差异写适配代码

**3.1 KeychainManager**
```swift
func save(key: String, value: String) {
    #if !SKIP
    // iOS: Security framework（现有代码不动）
    #else
    // Android: EncryptedSharedPreferences
    #endif
}
```

**3.2 生物认证**
```swift
func authenticateWithBiometrics(reason: String) async -> Bool {
    #if !SKIP
    // iOS: LAContext（现有代码）
    #else
    // Android: BiometricPrompt
    #endif
}
```

**3.3 推送通知**
```swift
#if !SKIP
// iOS: UNUserNotificationCenter（现有 ReminderService，不动）
#else
// Android: WorkManager + NotificationCompat
#endif
```

**3.4 统计图表**
```swift
// MoodInsightsView.swift
#if !SKIP
import Charts
// 现有 Swift Charts 代码
#else
// Compose Canvas 或 Vico 自绘图表
#endif
```

**3.5 SwiftData → Android Repository 实现**
```swift
#if SKIP
// SkipDB 或 Room 实现 HistoryRepository 协议
final class RoomHistoryRepository: HistoryRepository {
    // ... Room 实现
}
#endif
```

---

### 阶段 4：Android 视觉打磨（1-2 天）
> iOS 保留现有的 AppleIntelligence 彩虹动效和毛玻璃；Android 用 Material 3 对应物替换

**4.1 `AppleIntelligenceStyle.swift` 的处理**
```swift
// LiquidGlassPanel 在 Android 端映射为 Material 3 的 Surface + ElevatedCard
#if !SKIP
struct LiquidGlassPanel<Content: View>: View {
    // 现有毛玻璃实现（不动）
}
#else
struct LiquidGlassPanel<Content: View>: View {
    // Android: Material 3 ElevatedCard 风格
    var body: some View {
        content()
            .background(.surfaceContainerHigh)  // M3 色调
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
            .shadow(radius: 4)
    }
}
#endif
```

**4.2 TimelineView 动效**
- iOS：保留现有的彩虹旋转动效（`IntelligenceAmbientBackground`、`RainbowOrbitalRing`）
- Android：`TimelineView` 在 Skip 中映射为 Compose 的 `LaunchedEffect` + `animateFloatAsState`，用 Material You 动态色系替代彩虹色

**4.3 Android 动态颜色（Material You）**
- Skip 会把 `Color("AccentColor")` 等 asset color 映射为 M3 color scheme
- 可以在 Android 端启用 Dynamic Color，让 App 主色跟随系统壁纸

---

### 阶段 5：集成验证（持续）
- [ ] iOS 模拟器 + 真机测试所有功能
- [ ] Android 模拟器（API 34+）运行跑通
- [ ] 真实安卓手机测试 AI 请求、本地存储、密钥安全
- [ ] 检查 Material 3 动效流畅度（目标：和原生 Compose App 无差异）

---

## 关键技术决策记录

| 决策 | 结论 | 理由 |
|------|------|------|
| 跨平台框架 | Skip.tools | 单一 Swift 代码库，双端原生渲染，无 Web 容器 |
| 数据库 | iOS: SwiftData，Android: Room，通过 Repository 协议隔离 | 各端最优解，ViewModel 无感知 |
| 密钥存储 | iOS: Keychain，Android: EncryptedSharedPreferences | 各端系统最高安全级别 |
| UI 风格 | iOS: 毛玻璃 + 彩虹动效，Android: Material 3 + Material You | 不妥协，各端极致体验 |
| 图表 | iOS: Swift Charts，Android: Compose Canvas | `#if SKIP` 隔离，各端最优 |
| 生物认证 | iOS: Face ID，Android: BiometricPrompt | `#if SKIP` 隔离 |

---

## 风险与注意事项

> [!WARNING]
> **Skip 的 SwiftData 支持**：Skip 官方提供 `SkipDB` 作为 SwiftData 的跨平台替代（底层是 SQLite）。
> 但 `@Query` 宏目前在 Android 端**不支持**。这正是阶段 1 要先做 Repository 重构的原因——
> 把 `@Query` 从 View 里移走后，数据库层就可以完全用 `#if SKIP` 做平台隔离。

> [!IMPORTANT]
> **项目结构变化**：Skip 要求特定的项目结构（Package.swift + Sources 目录）。
> 迁移前建议在 Git 新开一个分支（如 `feature/skip-migration`），不要在 main 分支上直接动手。

> [!NOTE]
> **`import Charts` 的替代**：Skip 目前不支持 Swift Charts 转译到 Android。
> MoodInsightsView 的图表部分需要用 `#if SKIP` 分别实现。
> Android 端可以用 `Vico`（Compose 图表库，MPL-2.0 协议）或自己用 Canvas API 绘制折线图，工作量约 0.5-1 天。

> [!NOTE]
> **Skip 是付费商业软件**：个人项目有免费 tier，但如果 App 发布到 Play Store
> 需要确认当前的授权条款是否满足你的使用场景。

---

## 不需要动的东西（放心，不会破坏）

- 所有 `*Service.swift`（34 个 AI 相关服务文件）→ **一行不改**
- 所有 `PromptTemplates.swift` 中的提示词 → **一行不改**
- `AnalysisResult`、`AIProvider`、`ReframeMode` 等纯数据模型 → **一行不改**
- iOS 端的所有 SwiftUI 视图代码 → **一行不改**（Skip 只是在编译时额外生成 Android 版本）
- 现有的 AppleIntelligence 彩虹动效 → **iOS 上原样保留**

