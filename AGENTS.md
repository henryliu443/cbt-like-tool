# CBTReframe Agent 交接手册

> 这个文件是所有 AI agent（Antigravity、Codex 等）的**第一个必读文档**。
> 每次开始新会话、新任务前，先读完这个文件，再读 `docs/progress/` 下最新的进度快照。

---

## 项目基本信息

- **项目名**：CBTReframe（CBT 思维重构工具）
- **仓库路径**：`/Users/henry/cbt-like-tool-2`
- **当前平台**：iOS / iPadOS / macOS（SwiftUI + SwiftData）
- **目标**：通过 Skip.tools 迁移到 Android，实现单一 Swift 代码库、双端原生渲染
- **主要开发者**：Henry（独立开发者，个人使用）

---

## 战略方向（已确认，不需再讨论）

1. **保留 Swift/SwiftUI**：Apple 生态（Mac + iPad）继续用原生 SwiftUI
2. **不用 Flutter，不用 Web 容器**
3. **选定方案：[Skip.tools](https://skip.dev)**
   - iOS 端：100% 原生 SwiftUI，毛玻璃/液态玻璃/彩虹动效全部保留
   - Android 端：Skip 编译时将 SwiftUI 翻译为 Jetpack Compose，Material 3 原生渲染
4. **核心 AI 服务层（34 个 Service 文件）：一行不改**

详细战略规划见：`implementation_plan.md`

---

## 文档结构

```
cbt-like-tool-2/
├── AGENTS.md                    # 你正在看的这个文件，所有 agent 必读
├── implementation_plan.md       # 完整战略规划（阶段 0-5）
├── docs/
│   └── progress/
│       ├── YYYY-MM-DD_HHmm_snapshot.md  # 每次工作结束的进度快照
│       └── latest.md            # 符号链接或手动复制的最新快照
└── CBTReframe/                  # 源代码
```

---

## Agent 工作流程

### 开始新任务时（必须执行）

1. 读 `AGENTS.md`（此文件）
2. 读 `docs/progress/latest.md`（最新进度快照）
3. 读 `implementation_plan.md`（总体计划）
4. 确认当前所在阶段，继续未完成的任务

### 执行任务期间（重要防忘机制）

- **定期重新读取计划**：在对话轮次较多、或者即将调用/启动其他 Agent 时，**必须不定期重新读取一次** `implementation_plan.md`。
- **原因**：防止长时间对话导致 Context 发生压缩（Compaction），导致 Agent 遗忘具体技术路线和设计细节。

### 结束工作时（必须执行）

在 `docs/progress/` 目录下创建新的快照文件，命名格式：
`YYYY-MM-DD_HHmm_snapshot.md`

快照必须包含：
- 本次完成了什么
- 当前阶段状态（哪些 ✅ 完成，哪些 🔄 进行中，哪些 ⬜ 未开始）
- 遇到的关键决策或问题
- 下一个 agent 应该从哪里开始
- 任何需要注意的坑或上下文

### 快照模板

```markdown
# 进度快照 YYYY-MM-DD HH:MM

## 本次完成
- ...

## 当前阶段状态
- 阶段 0（环境准备）：✅ / 🔄 / ⬜
- 阶段 1（Repository 重构）：✅ / 🔄 / ⬜
- 阶段 2（Skip 初始化）：✅ / 🔄 / ⬜
- 阶段 3（平台适配层）：✅ / 🔄 / ⬜
- 阶段 4（Android 视觉打磨）：✅ / 🔄 / ⬜
- 阶段 5（集成验证）：✅ / 🔄 / ⬜

## 关键决策
- ...

## 已知问题 / 坑
- ...

## 下一步（下一个 agent 从这里开始）
- ...

## 重要文件变动
- 新增：...
- 修改：...
- 删除：...
```

---

## 多智能体协作机制 (Multi-Agent Orchestrator Principle)

### 核心协作原则
- **动态路由与拆分**：所有任务默认先进行复杂度评估与拆分，优先采用多智能体协作而非单模型串行执行。根据任务规模动态路由至对应能力层级（Low→简单执行与信息收集，Medium→中等复杂分析与实现，High/Primary→架构设计、最终决策与全局协调）。
- **职责分离原则**：每个子任务必须包含明确的 **Executor（执行者）** 与 **Verifier（验证者）** 角色。严禁由同一 Agent 自我验收。
- **验收闭环机制**：任何任务状态不得以“Executor 声明完成”为完成依据，只有 Verifier 确认满足验收标准后方可进入下一阶段。Primary Agent 必须读取并确认 Verifier 反馈，而非直接读取 Executor 结论。
- **固定任务流转**：流程固定为 `Plan → Execute → Verify → Approve → Continue`。任何阶段验收失败均返回上一阶段修正。
- **输出规范**：所有 Agent 输出均需附带：`完成状态`、`未解决问题`、`风险项` 与 `置信度`。

### 角色分配

| 角色 | 适用模型/Agent | 在本项目中的职责 |
|------|--------------|----------------|
| **Primary Agent (Coordinator/Approver)** | Antigravity (Gemini Pro - High) | 负责全局协调、仲裁与最终批准。**不应亲自执行可下放的任务**，以最大化并行度、成本效率与结果可靠性。 |
| **Executor** | 子智能体 (Codex, Gemini Low) | 负责根据计划产出代码结果，执行单一或中低复杂度的开发/收集任务。 |
| **Verifier** | 独立的子智能体 (Codex, Gemini Low) | 负责独立验收 Executor 的产出。优先使用独立模型，若首选不可用则降级至备用池。**例外机制**：若所有备用 Verifier 均不可用（如额度耗尽），则作为最后手段，由 Primary Agent 亲自承担验证职责。 |

### 执行与验证流程

```text
Primary Agent 制定计划 (Plan)
  → 下放给 Executor 执行 (Execute)
  → 指派独立的 Verifier 进行验收 (Verify)
  → Primary Agent 读取 Verifier 报告并裁决 (Approve)
  → 更新快照，进入下一阶段 (Continue)
```

---

## 当前技术栈速查

| 层 | iOS（当前）| Android（目标，Skip 生成）|
|----|-----------|--------------------------|
| UI | SwiftUI | Jetpack Compose（Skip 翻译）|
| 数据库 | SwiftData | Room（通过 Repository 接口）|
| 密钥存储 | iOS Keychain | EncryptedSharedPreferences |
| 网络 | URLSession | URLSession（SkipFoundation 支持）|
| 生物认证 | LocalAuthentication (Face ID) | BiometricPrompt |
| 推送 | UNUserNotificationCenter | WorkManager + NotificationCompat |
| 图表 | Swift Charts | Vico 或 Canvas 自绘 |
| 架构 | MVVM + @Observable | 同左（Skip 转译）|

---

## 绝对不能动的文件（除非有明确指令）

- `CBTReframe/Services/*.swift`（34 个 AI 服务文件）
- `CBTReframe/Services/PromptTemplates.swift`（提示词）
- `CBTReframe/Views/Components/AppleIntelligenceStyle.swift`（iOS 专属动效，Android 端另行适配）
- 所有 `*Model.swift` 纯数据结构文件

---

## Git 工作规范

- 迁移工作在 `feature/skip-migration` 分支进行
- `main` 分支保持 iOS 可运行状态
- 每个阶段完成后 merge 到 main 并打 tag（如 `v2.0-phase1`）
