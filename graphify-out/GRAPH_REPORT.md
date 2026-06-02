# Graph Report - /Users/henry/cbt-like-tool/CBTReframe  (2026-06-02)

## Corpus Check
- Corpus is ~23,433 words - fits in a single context window. You may not need a graph.

## Summary
- 505 nodes · 663 edges · 38 communities detected
- Extraction: 83% EXTRACTED · 17% INFERRED · 0% AMBIGUOUS · INFERRED: 112 edges (avg confidence: 0.5)
- Token cost: 0 input · 0 output

## God Nodes (most connected - your core abstractions)
1. `CognitiveDistortion` - 16 edges
2. `CognitiveDistortion` - 15 edges
3. `ReframeViewModel` - 12 edges
4. `AIProvider` - 12 edges
5. `PromptBuilder` - 12 edges
6. `AIServiceError` - 12 edges
7. `SettingsViewModel` - 11 edges
8. `ThinkingTemplate` - 11 edges
9. `AnalysisDepth` - 10 edges
10. `AppResponseStyle` - 10 edges

## Surprising Connections (you probably didn't know these)
- `ResultCardView` --inherits--> `View`  [EXTRACTED]
  /Users/henry/cbt-like-tool/CBTReframe/Views/ResultCardView.swift →   _Bridges community 0 → community 2_
- `HomeView` --inherits--> `View`  [EXTRACTED]
  /Users/henry/cbt-like-tool/CBTReframe/Views/HomeView.swift →   _Bridges community 0 → community 14_
- `HistoryView` --inherits--> `View`  [EXTRACTED]
  /Users/henry/cbt-like-tool/CBTReframe/Views/HistoryView.swift →   _Bridges community 0 → community 12_
- `MoodTagPicker` --inherits--> `View`  [EXTRACTED]
  /Users/henry/cbt-like-tool/CBTReframe/Views/Components/MoodTagPicker.swift →   _Bridges community 0 → community 11_
- `LiquidGlassPanel` --inherits--> `View`  [EXTRACTED]
  /Users/henry/cbt-like-tool/CBTReframe/Views/Components/AppleIntelligenceStyle.swift →   _Bridges community 0 → community 13_

## Communities

### Community 0 - "Entry Point & App Setup"
Cohesion: 0.05
Nodes (26): App, CBTReframeApp, DailyMoodPoint, ExerciseGuide, ExerciseGuideView, ExercisesView, MainTabView, MoodInsightsView (+18 more)

### Community 1 - "Enum Protocols & Distortions"
Cohesion: 0.05
Nodes (37): CaseIterable, Codable, CognitiveDistortion, blackAndWhite, catastrophizing, emotionalReasoning, fortuneTelling, labeling (+29 more)

### Community 2 - "Analysis Result Model"
Cohesion: 0.07
Nodes (21): AnalysisResult, CodingKeys, action, actions, alternative, distortion, questions, stateAssessment (+13 more)

### Community 3 - "AI Service Protocol"
Cohesion: 0.08
Nodes (7): AIServiceProtocol, AIServiceProtocol, AnthropicService, DeepSeekService, GeminiService, LocalAnalysisService, MoonshotService

### Community 4 - "Model List Service"
Cohesion: 0.09
Nodes (21): AIModelListError, decodeFailed, httpStatus, invalidURL, AIModelListService, GeminiModelsResponse, Item, Model (+13 more)

### Community 5 - "Session & Global Settings"
Cohesion: 0.1
Nodes (16): AppSession, AnalysisDepth, balanced, deep, fast, AppResponseStyle, coach, concise (+8 more)

### Community 6 - "Prompt Templates"
Cohesion: 0.11
Nodes (12): CognitiveDistortion, blackAndWhite, catastrophizing, emotionalReasoning, fortuneTelling, labeling, mentalFilter, mindReading (+4 more)

### Community 7 - "Risk Routing & Safety"
Cohesion: 0.12
Nodes (19): Equatable, calculateRiskScore(), CrisisLocalSupport, detectRiskLevel(), hasImmediateCrisisKeyword(), ResponseStrategy, cbtGentle, cbtNormal (+11 more)

### Community 8 - "LLM Provider Bridge"
Cohesion: 0.12
Nodes (12): AIServiceLLMProvider, CodingKeys, hasAkathisia, mode, mood, strategy, style, template (+4 more)

### Community 9 - "Settings ViewModel"
Cohesion: 0.19
Nodes (3): DefaultReminderScheduler, ReminderScheduling, SettingsViewModel

### Community 10 - "Reframe ViewModel"
Cohesion: 0.18
Nodes (5): LoadingBannerStyle, deepReasoningWithTimer, geminiPro, none, ReframeViewModel

### Community 11 - "AI Provider Models"
Cohesion: 0.12
Nodes (11): AIModel, AIProvider, anthropic, deepseek, gemini, kimi, local, openai (+3 more)

### Community 12 - "History View"
Cohesion: 0.15
Nodes (4): HistoryExportActivityView, HistoryRowView, HistoryView, UIViewControllerRepresentable

### Community 13 - "Apple Intelligence Style"
Cohesion: 0.21
Nodes (9): IntelligenceAmbientBackground, IntelligenceAnimatedGlyph, IntelligenceRainbow, IntelligenceRainbowCardStroke, LiquidGlassPanel, RainbowEdgeGlow, RainbowOrbitalRing, View (+1 more)

### Community 14 - "Home View"
Cohesion: 0.17
Nodes (7): HomeFlowStep, chooseMode, chooseMood, writeThought, HomeView, StreamingResultView, Int

### Community 15 - "OpenAI Service"
Cohesion: 0.31
Nodes (6): OpenAIService, parseJSONContent(), parsePlainTextCrisisResponse(), parseReframeOutput(), parseStringArray(), parseThoughtPatternContent()

### Community 16 - "Analysis Engine"
Cohesion: 0.2
Nodes (6): AnalysisEngine, AnalysisEngine, AnalysisEngineRequest, BehavioralEngine, CBTEngine, SocraticEngine

### Community 17 - "Reframe Pipeline"
Cohesion: 0.25
Nodes (4): AnalysisInputEnvelope, AnalysisRunMetadata, ReframePipeline, ReframePipelineOutput

### Community 18 - "History Export"
Cohesion: 0.43
Nodes (1): HistoryExportService

### Community 19 - "Keychain Manager"
Cohesion: 0.38
Nodes (1): KeychainManager

### Community 20 - "External AI Launcher"
Cohesion: 0.48
Nodes (1): ExternalAIAppLauncher

### Community 21 - "History ViewModel"
Cohesion: 0.4
Nodes (1): HistoryViewModel

### Community 22 - "Reframe Output Gate"
Cohesion: 0.6
Nodes (1): ReframeOutputGate

### Community 23 - "Reframe UseCase"
Cohesion: 0.47
Nodes (2): ReframeUseCase, ReframeUseCaseOutput

### Community 24 - "Streak Service"
Cohesion: 0.33
Nodes (1): StreakService

### Community 25 - "Thought Journal ViewModel"
Cohesion: 0.4
Nodes (1): ThoughtJournalViewModel

### Community 26 - "Haptic Manager"
Cohesion: 0.4
Nodes (1): HapticManager

### Community 27 - "Reminder Service"
Cohesion: 0.4
Nodes (1): ReminderService

### Community 28 - "Socratic Validation"
Cohesion: 0.67
Nodes (1): SocraticPipelineValidation

### Community 29 - "Thought Pattern Pipeline"
Cohesion: 0.5
Nodes (1): ThoughtPatternPipeline

### Community 30 - "LLM JSON Sanitizer"
Cohesion: 0.67
Nodes (1): LLMJSONSanitizer

### Community 31 - "Retry Executor"
Cohesion: 0.67
Nodes (2): ReframeRetryExecutor, RetryExecutionResult

### Community 32 - "AI Provider Resolver"
Cohesion: 0.67
Nodes (2): AIProviderResolver, SettingsAIProviderResolver

### Community 33 - "Engine Router"
Cohesion: 0.67
Nodes (1): EngineRouter

### Community 34 - "Validated Runner"
Cohesion: 0.67
Nodes (1): RetriableValidatedReframeRunner

### Community 35 - "AI Service Factory"
Cohesion: 0.67
Nodes (1): AIServiceFactory

### Community 36 - "App Metadata"
Cohesion: 1.0
Nodes (1): AppMetadata

### Community 37 - "App Logger"
Cohesion: 1.0
Nodes (1): AppLogger

## Knowledge Gaps
- **98 isolated node(s):** `AppMetadata`, `none`, `deepReasoningWithTimer`, `geminiPro`, `quick` (+93 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **Thin community `App Metadata`** (2 nodes): `AppMetadata.swift`, `AppMetadata`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `App Logger`** (2 nodes): `AppLogger.swift`, `AppLogger`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `FollowUpChatView` connect `Analysis Result Model` to `Entry Point & App Setup`?**
  _High betweenness centrality (0.111) - this node is a cross-community bridge._
- **Why does `CognitiveDistortion` connect `Prompt Templates` to `Entry Point & App Setup`, `Enum Protocols & Distortions`, `Analysis Result Model`?**
  _High betweenness centrality (0.056) - this node is a cross-community bridge._
- **Why does `ResponseStrategy` connect `Risk Routing & Safety` to `Enum Protocols & Distortions`?**
  _High betweenness centrality (0.047) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `String` (e.g. with `.normalized()` and `.init()`) actually correct?**
  _`String` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `AppMetadata`, `none`, `deepReasoningWithTimer` to the rest of the system?**
  _98 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Entry Point & App Setup` be split into smaller, more focused modules?**
  _Cohesion score 0.05 - nodes in this community are weakly interconnected._
- **Should `Enum Protocols & Distortions` be split into smaller, more focused modules?**
  _Cohesion score 0.05 - nodes in this community are weakly interconnected._