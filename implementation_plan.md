# Android KMP 重构执行计划 (终局垂直切片版)

经过多轮工作坊的深度推演与 Codex (DeepSeek V4 Pro) 的架构纠偏，我们摒弃了“先写纯逻辑再写UI”这种容易导致“盲写期过长、无法端到端测试”的水平分层架构，转向**极度务实的“垂直切片 (Vertical Slicing)”敏捷开发模式**。

## 架构核心共识
1. **ViewModel 与 Business 合二为一**：在 KMP 中，`ViewModel`（基于 `StateFlow`）本身就是纯粹的 Kotlin 业务层，不属于视图层。强行在 Repository 和 ViewModel 之间抽离“纯业务层”是过度设计。
2. **UI 框架维持双端原生**：Android 端使用 Jetpack Compose，iOS 端保留原生的 SwiftUI。只共享业务逻辑 (Shared Core) 而不共享 UI，风险最低。
3. **每个阶段必须是 Runnable Demo**：避免长时间憋大招，每个 Phase 结束时都必须能在设备上跑出画面，第一时间暴露架构问题。

---

## 垂直切片执行路径 (Vertical Slice Execution Path)

### Phase 1 (MVP 切片): 最简核心链路跑通
**目标**：证明在共享架构下，AI 的流式渲染 (SSE) 能在安卓上稳定跳动。
- **Shared Core (`shared`)**:
  - `AIProvider` 与 `AIModel` 枚举。
  - `AIService` 接口设计（返回 `Flow<String>`）。
  - **网络层落地**：优先使用 Ktor Client 底层的 `bodyAsChannel()` 手动硬解析大模型流式响应（避开不稳定的 SSE 插件）。若遇阻，即刻降级为工厂模式封装 Android `OkHttp`。
  - 最简 `ReframeViewModel`（剥离数据库依赖，只处理网络回包状态）。
- **Android UI (`androidApp`)**:
  - 搭建极简的 Compose 骨架（一个输入框，一个输出流渲染框），通过 `collectAsState()` 订阅 ViewModel。
*验收标准*：能在模拟器发出测试请求，看到大模型文字逐字流式渲染，证明【网络 -> ViewModel -> UI】主干打通。

### Phase 2 (MVP+ 切片): 本地持久化与复杂业务
**目标**：把临时数据转为持久化数据，补全历史记录拼图。
- **Shared Core (`shared`)**:
  - 引入 **SQLDelight** 搭建跨平台数据库，全量映射原先的 `SwiftData` 结构。
  - 完善 `HistoryRepository` 和 `HistoryViewModel`。
- **Android UI (`androidApp`)**:
  - 用 Compose 开发完整的历史记录列表页、卡片 UI 还原。
*验收标准*：AI 的返回结果能落入数据库，App 重启后历史记录依然可见。

### Phase 3 (Polish 润色): 平台特性与安全基建
**目标**：填补必须调用操作系统底层 API 的空白。
- **Shared Core (`shared`)**:
  - 引入 `Multiplatform-Settings (Encrypted)` 取代 iOS Keychain 存储 API Key。
  - 抽象 `BiometricAuthProvider` 接口。
  - 引入 `Koin` 进行依赖注入的全局统筹。
- **Android UI (`androidApp`)**:
  - 编写基于 Android `BiometricPrompt` 的真实实现并经 Koin 注入。
  - 补齐全套卡片渐变、弹性动画和过渡效果。
*验收标准*：能够实现首次启动引导配置 Key、能够通过人脸/指纹解锁私密记录。

---

## Agent 协作纪律回顾
- **Primary Agent (Antigravity)** 负责拆解 Phase、发布断点检查指令、做最终 Approve。
- **Executor** 负责基于本路线图逐个切片编写代码，默认通道 Flash，硬核代码升 Codex。
- **Verifier** 在每个切片完结时独立运行 `gradle assembleDebug`，只要不能 Run 的代码一律打回。
