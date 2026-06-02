# Android 迁移与分支重组多智能体计划

根据最新的 `AGENT.md` 规范，本次迁移被严格拆分为明确的 Stage 和 Phase。每个 Phase 严格遵循 `Plan → Execute → Verify → Approve` 的流转机制。

> **自我反省机制**：在每个 Stage 结束时，所有相关的 Agent 必须回顾 `AGENT.md` 和本计划，灵魂拷问：我现在要干嘛？之前在干嘛？我的职责边界在哪里？

## 严格角色边界分配
- **Primary Agent (Gemini 3.1 Pro High, 我)**：**仅治理不开发**。负责拆分任务、制定和更新此计划文档、定义验收标准、审阅 Verifier 结果并决定 Approve 或 Reject。绝对禁止我直接使用终端或修改代码文件。
- **Executor**：**负责干活**。
  - **默认通道 (Gemini Flash)**：用于简单的 Git 分支重命名、文件删除、读目录等基础操作。
  - **升级通道 (MCP Codex, DeepSeek V4 Pro)**：仅当涉及 Android 架构搭建、多文件代码生成时才被唤醒使用。
- **Verifier (Gemini 3.5 Flash)**：**只验收不设计**。负责执行检查命令，判断 PASS / FAIL。严禁其修改代码或提出架构新建议。

---

## Stage 1: 分支备份与工作区清理 (Git Branch & Backup)

本阶段主要是基础的 Git 命令和文件删除操作，属于简单任务，**Executor 默认采用 Gemini Flash**。

### Phase 1.1: 备份当前 iOS 状态
- **Execute (Gemini Flash)**: 执行 `git branch -m main ios` 将当前主分支更名为 `ios` 作为永久备份。
- **Verify (Gemini 3.5 Flash)**: 运行 `git branch` 确认 `ios` 分支存在且为当前分支。输出 PASS/FAIL。
- **Approve (Primary)**: 阅读 Verifier 报告，确认 iOS 代码备份万无一失。

### Phase 1.2: 新开 Android 分支与清理
- **Execute (Gemini Flash)**:
  1. 执行 `git checkout -b android` 创建并切换到安卓专属分支。
  2. 执行 `git rm -rf CBTReframe CBTReframe.xcodeproj CBTReframeTests README.md`，清理工作区中的 iOS 特有文件。
- **Verify (Gemini 3.5 Flash)**: 运行 `ls -la` 和 `git status` 确认当前处于 `android` 分支且目录纯净。输出 PASS/FAIL。
- **Approve (Primary)**: 确认工作区已对 Android 迁移处于"纯净"状态。

---

## Stage 2: Android 基础工程搭建 (Initialization)

本阶段涉及平台迁移和大量的样板代码生成联动，复杂度高，**Executor 允许升级调用 MCP Codex**。

### Phase 2.1: 构建系统与根目录配置
- **Execute (MCP Codex)**:
  1. 创建 `settings.gradle.kts`，配置项目名。
  2. 创建根目录 `build.gradle.kts`，配置 Kotlin、Android 插件版本。
  3. 创建基础 Gradle 运行库文件。
- **Verify (Gemini 3.5 Flash)**: 验证生成的构建脚本语法是否正确。输出 PASS/FAIL/FAIL REASON。
- **Approve (Primary)**: 确认根目录结构符合现代 Android 规范。

### Phase 2.2: App 模块与 Jetpack Compose 接入
- **Execute (MCP Codex)**:
  1. 创建 `app/build.gradle.kts`，配置包名 `com.henryliu.cbtreframe` 和 Jetpack Compose。
  2. 创建 `AndroidManifest.xml` 和 `MainActivity.kt` 包含空白启动界面。
- **Verify (Gemini 3.5 Flash)**: 运行 `gradlew clean assembleDebug` 执行 **Build Green** 检查。输出 PASS/FAIL。
- **Approve (Primary)**: 确认编译通过。

---

## Stage 3: 快照更新与阶段总结 (Snapshot & Continue)

### Phase 3.1: 提交与流转记录
- **Execute (Gemini Flash)**: 执行 `git add .` 和 `git commit -m "chore: setup android jetpack compose project"`。
- **Verify (Gemini 3.5 Flash)**: 检查 Git 提交历史。
- **Approve (Primary)**: 更新进度快照。

---

## User Review Required

> [!IMPORTANT]
> 1. 已将您的 3 条绝对红线规则写入 `AGENT.md` 和本计划。
> 2. 计划中明确了 Stage 1（Git）由于较简单分配给 Gemini Flash，Stage 2（安卓搭建）较复杂分配给 Codex。
> 3. 我（Primary）已交出执行权，完全回到治理者位面。
>
> 如果规则落实到位，请随时发令启动执行，我们将严格按此秩序推进。
