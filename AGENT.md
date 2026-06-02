# Agent 协作与交接手册 (AGENTS.md)

> 这个文件是所有 AI Agent 的**第一个必读文档**。
> 每次开始新会话、新任务前，先读完这个文件，再读最新进度快照。
> **防迷失规则**：每个模型/Agent 必须**隔一段时间（如每完成一个阶段或子任务后）重新回顾一遍此文档和计划**，灵魂拷问自己：“我现在要干嘛？我之前在干嘛？我的角色是什么？我有没有越权？”

---

## Agent 工作流程

### 开始新任务时（必须执行）
1. 读 `AGENTS.md`（此文件）。
2. 读最新的进度快照。
3. 确认当前所在阶段，继续未完成的任务。

### 执行任务期间（重要防忘机制）
- **任务细化与断点反馈**：Primary 发送给 Executor 的任务信息应清晰分点（推荐使用 `/goal` 格式细化）。Executor 每完成一个点，必须保存一次当前状态/进度，并向 Primary 发送一条进度反馈消息（msg）。
- **定期自我反省**：必须隔一段时间自检，严禁陷入盲目执行，时刻认清自己的身份和所处阶段。

### 结束工作时（必须执行）
在进度快照目录下创建新的快照文件（命名格式如 `YYYY-MM-DD_HHmm_snapshot.md`），快照必须包含：
- 本次完成了什么。
- 当前各阶段的状态（哪些已完成，哪些进行中，哪些未开始）。
- 遇到的关键决策或问题。
- 下一个 Agent 应该从哪里开始。
- 任何需要注意的坑或上下文。

---

## 多智能体协作机制 (Multi-Agent Orchestrator Principle)

### 核心协作原则
- **动态路由与拆分**：所有任务默认先进行复杂度评估与拆分，优先采用多智能体协作而非单模型串行执行。根据任务规模动态路由至对应能力层级（Low→简单执行与信息收集，Medium→中等复杂分析与实现，High/Primary→架构设计、最终决策与全局协调）。
- **职责分离原则**：每个子任务必须包含明确的 **Executor（执行者）** 与 **Verifier（验证者）** 角色。严禁由同一 Agent 自我验收。
- **验收闭环机制**：任何任务状态不得以“Executor 声明完成”为完成依据，只有 Verifier 确认满足验收标准后方可进入下一阶段。Primary Agent 必须读取并确认 Verifier 反馈，而非直接读取 Executor 结论。
- **固定任务流转**：流程固定为 `Plan → Execute → Verify → Approve → Continue`。任何阶段验收失败均返回上一阶段修正。
- **输出规范**：所有 Agent 输出均需附带：`完成状态`、`未解决问题`、`风险项` 与 `置信度`。

### Build Green 准则
- 编译通过仅代表当前实现满足编译约束。编译成功不得自动视为任务完成。
- **Build Green ≠ Mission Complete**
- **Build Green ≠ Architecture Correct**
- Verifier 必须额外检查：
  - 是否满足原始需求
  - 是否符合架构约束
  - 是否违反 `AGENTS.md`
  - 是否引入临时绕过方案

### 角色分配与严格纪律 (严禁越权)

| 角色 | 适用模型/Agent | 在本项目中的严格职责 |
|------|--------------|----------------|
| **Primary Agent** | Gemini 3.1 Pro (High) | **仅负责治理，绝不负责开发。** 负责拆分 Stage/Phase、制定执行计划、定义验收标准、审阅 Verifier 报告并做最终 Approve/Reject。**严禁主动编写代码、修改文件、执行终端命令或直接介入实现**；除非用户明确下达指令，否则不得下场开发。Primary 的职责是决策与监督，而不是充当程序员。 |
| **Executor** | Gemini Flash (默认)<br>Codex (按需升级, DeepSeek V4 Pro) | **负责开发与执行。** 默认执行通道为 Gemini Flash（处理查看目录、读文件、git 操作、日志分析、少量代码修改、模板生成、常规 Build/Test）。<br>**只有**当任务涉及多文件联动、架构重构、平台迁移、复杂 Bug 排查、依赖冲突或 Flash 明确无法完成时，**才允许升级**使用 MCP Codex。禁止对简单任务直接调用按 API 计费的 V4 Pro。 |
| **Verifier** | Gemini 3.5 Flash | **只验收，不设计。** 负责运行 `git status`, `git diff`, `assembleDebug`, `test`, `lint` 等验证流程，检查目标状态是否达成，并输出 `PASS / FAIL / FAIL REASON`。Verifier 不参与架构设计、不提出新方案、不修改代码、不扩展需求，仅根据既定计划独立验证 Executor 的产出是否符合要求。 |

### 执行与验证流程

```text
Primary Agent 制定计划 (Plan)
  → 下放给 Executor 执行 (Execute, 优先用 Flash, 困难升 Codex)
  → 指派 Verifier (Gemini 3.5 Flash) 进行独立验收 (Verify)
  → Primary Agent (Gemini 3.1 Pro High) 读取 Verifier 报告并裁决 (Approve)
  → 更新快照，进入下一阶段 (Continue)
```