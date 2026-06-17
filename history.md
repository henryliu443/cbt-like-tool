# Android HistoryView 迁移任务（Phase 1：功能对齐）



## 1. 历史记录安全锁定机制迁移



当前 Android 仅实现最基础的 BiometricPrompt 验证，而 iOS 已形成完整的历史记录保护流程，包括锁定页面、错误提示、重新验证、禁用保护以及后台自动重新锁定等行为。需要完整迁移 isUnlocked、authErrorMessage、hasAttemptedAuth、scenePhase 锁定逻辑，并补齐独立 LockedState 页面。目标是 Android 历史记录保护行为与 iOS 完全一致，而非仅实现一次生物识别弹窗。



## 2. 历史记录工具栏功能迁移



当前 Android 顶部仅显示标题，而 iOS 已支持收藏过滤与导出功能。需要增加 Toolbar Actions，包括收藏筛选按钮、导出按钮以及对应状态管理逻辑。收藏筛选应能够切换全部记录与仅收藏记录；导出按钮应进入格式选择流程。目标是让 Android 用户拥有与 iOS 相同的数据管理能力。



## 3. 历史记录导出系统迁移



iOS 已支持 JSON、CSV、PDF 三种导出格式，并通过系统分享面板进行分发。Android 目前完全缺失导出能力。需要实现 Export Dialog、文件生成逻辑以及 Android Share Sheet 集成，同时保证导出内容结构与 iOS 保持一致。迁移完成后用户能够导出全部历史记录，而不仅限于单条内容。



## 4. Weekly Review Card 迁移



Android 当前直接显示历史列表，而 iOS 在顶部提供 Weekly Review Card，展示本周分析次数、收藏数量、总记录数以及最常见心情。需要迁移 weeklyStats()、topMoodEmoji 等统计逻辑，并实现对应卡片 UI。该模块是历史页面的重要入口信息，能够帮助用户快速了解近期使用情况。



## 5. History Row Metadata 完整迁移



Android 当前仅显示输入内容与时间，而 iOS 已显示模板标签、心情标签、分析深度标签、认知扭曲标签以及 Provider 信息。需要补齐所有 Metadata 展示逻辑，并统一标签样式与视觉层级。目标是让用户在列表层即可快速辨认历史记录类型，而无需逐条展开查看。



## 6. 收藏系统迁移



iOS 支持历史记录收藏、取消收藏以及收藏筛选，Android 当前完全缺失。需要迁移 Favorite 状态字段、收藏按钮、收藏筛选逻辑以及对应持久化流程。该功能直接影响用户对高价值分析结果的长期保存能力，应视为核心功能而非增强功能。



## 7. 分享功能迁移



iOS 支持单条历史记录导出与分享，并根据不同 ThinkingTemplate 自动生成结构化内容。Android 当前无分享能力。需要迁移 buildShareText() 逻辑以及系统分享入口，同时保证 CBT、Socratic、Behavioral 三种模板均能正确生成内容。目标是实现跨平台一致的分享体验。



## 8. Context Menu 与 Swipe Actions 对齐



iOS 已支持长按菜单与侧滑操作，用户可快速执行分享与删除。Android 当前只能展开后点击删除按钮。建议迁移长按菜单、快捷操作入口以及删除确认流程，减少用户操作路径。目标是保持双端交互能力一致。



## 9. History Detail 展开内容迁移



Android 当前展开后仅显示 AI Response，而 iOS 展开后显示完整 ResultCardView，包括分析结果、行动建议、替代想法等结构化内容。需要迁移 ResultCardView 对应 Android 实现，并保证三种 ThinkingTemplate 都能够正确渲染。该项属于 History 页面最核心的信息展示能力。



## 10. Empty State 对齐



iOS Empty State 包含图标、标题与引导文案，而 Android 当前仅显示一句“还没有历史记录”。建议补齐完整 Empty State 设计，使首次使用体验更加完整一致。


# Phase 2 — History Metadata、Detail、Share、Export 完整迁移

## 1. HistoryEntryCard 信息结构重建

当前 Android 历史记录卡片仅展示输入内容与时间戳，信息量严重不足。需要完全参考 iOS HistoryRowView，重新设计卡片头部结构。每条记录至少显示 ThinkingTemplate 标签、Mood 标签、AnalysisDepth 标签、Distortion 标签、Provider 信息以及时间信息。标签统一采用 Capsule 风格，避免纯文本堆叠。目标是在不展开记录的情况下，用户即可快速识别该分析属于什么类型、什么心情、什么分析深度以及使用了哪个模型服务商。

## 2. 收藏系统完整迁移

迁移 iOS 的收藏功能，包括收藏按钮、收藏状态持久化、收藏筛选以及收藏视觉反馈。每条记录右上角增加 Star Icon，支持收藏与取消收藏。收藏状态必须写入数据库而非内存状态，确保应用重启后依然保留。顶部 Toolbar 增加“仅显示收藏”模式切换按钮。收藏筛选必须实时更新列表而无需刷新页面。所有历史数据兼容旧版本数据库，不允许因字段迁移导致旧记录失效。

## 3. History Detail 结构化内容迁移

当前 Android 展开后仅显示 AI 返回文本，本质上只是字符串查看器。需要完整迁移 iOS 的 ResultCardView 架构，将分析结果按照结构化模块展示。CBT 模板展示认知扭曲、替代想法和行动建议；Socratic 模板展示问题列表、反思说明和练习内容；Behavioral 模板展示状态评估、积极视角和下一步行动。目标是让 Android 用户获得与 iOS 完全一致的阅读体验，而不是阅读未经组织的大段文本。

## 4. Metadata 数据兼容与异常处理

迁移过程中需要统一处理历史记录缺失字段问题。MoodTag 为空时自动隐藏标签；ProviderName 缺失时隐藏 Provider 区域；AnalysisDepth 缺失时使用兼容展示逻辑；ThinkingTemplate 为空时默认回退至 CBT。所有历史旧记录必须正常显示，不允许出现 null、Unknown、空胶囊标签或应用崩溃。重点检查数据库迁移后历史数据是否仍然可读。

## 5. 单条记录分享系统迁移

迁移 iOS buildShareText() 逻辑，实现单条历史记录分享能力。根据不同 ThinkingTemplate 动态生成不同格式内容，而非统一导出 AI 回复。CBT 输出认知扭曲、替代想法和行动建议；Socratic 输出问题列表和反思内容；Behavioral 输出状态评估和行动计划。通过 Android Share Sheet 对接系统分享功能，允许用户发送到微信、邮件、备忘录等第三方应用。

## 6. 历史记录导出功能迁移

在 Toolbar 中增加导出入口，点击后弹出格式选择器。支持 JSON、CSV、PDF 三种导出格式，并与 iOS 保持字段结构一致。JSON 用于数据备份与恢复；CSV 用于 Excel 分析；PDF 用于阅读和打印。导出内容包含时间、输入内容、情绪标签、模板类型、分析深度、分析结果及相关元数据。导出完成后自动调用系统分享面板，无需用户手动寻找文件。

## 7. Context Menu 与快捷操作迁移

迁移 iOS 长按菜单能力。长按历史记录卡片后显示操作菜单，包括分享和删除两项核心操作。菜单结构与 iOS 保持一致，避免 Android 出现独立交互逻辑。所有快捷操作必须复用统一业务逻辑，不允许 Context Menu、Toolbar 和详情页分别维护不同实现。目标是减少重复代码并降低未来维护成本。

## 8. Swipe Actions 迁移

参考 iOS SwipeActions，为历史记录增加侧滑快捷操作。用户侧滑后可直接执行分享或删除操作。删除行为必须先进入确认对话框，禁止误触即删除。侧滑动作需要兼容长列表场景，不影响正常滚动体验。视觉风格尽量靠近 iOS 当前实现，同时符合 Material3 交互规范。

## 9. 删除流程统一治理

当前 Android 删除逻辑分散在多个位置，后续迁移完成后需要统一删除入口。无论用户通过 Context Menu、Swipe Action、详情页按钮还是未来其他入口触发删除，都必须经过统一确认弹窗。确认后统一调用同一个 ViewModel 删除流程，保证行为一致。重点检查数据库删除、UI刷新以及分组列表更新逻辑，避免删除后界面残留旧数据。

## 10. Critic 审查与验收要求

Critic 必须逐项核对 Android 与 iOS 的 HistoryView、HistoryRowView、ResultCardView 和导出逻辑。重点检查收藏状态恢复、模板渲染差异、分享文本一致性、CSV 编码问题、PDF 中文支持以及数据库兼容性。禁止使用 Mock 数据、TODO、Placeholder、临时回退逻辑或硬编码文本作为交付结果。只有在 Android 行为与 iOS 基本一致的前提下才允许进入 Judge 审核阶段。