package com.henryliu.cbtreframe.shared

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class ReframeUiState(
    val inputText: String = "",
    val selectedMood: String = "",
    val isAkathisia: Boolean = false,
    val result: AnalysisResult? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showCrisisBanner: Boolean = false,
    val isButtonPressed: Boolean = false,
    val retryRecoveryNotice: String? = null,
    val streamingText: String = "",
    val isStreamingResult: Boolean = false,
    val latestHistoryEntryID: String? = null,
    val analysisElapsedSeconds: Int = 0,
    val thinkingPhraseIndex: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val todayAnalysisCount: Int = 0,
    val suggestedThinkingTemplate: ThinkingTemplate? = null,
    val selectedProvider: AIProvider = AIProvider.LOCAL,
    val selectedModelName: String = "",
    val selectedTemplate: ThinkingTemplate? = null,
) {
    val greeting: String
        get() {
            val hour = kotlinx.datetime.Clock.System.now()
                .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).hour
            return when (hour) {
                in 5..11 -> "早上好"
                in 12..13 -> "中午好"
                in 14..17 -> "下午好"
                in 18..21 -> "晚上好"
                else -> "夜深了"
            }
        }

    val todayQuote: String
        get() {
            val dayOfYear = kotlinx.datetime.Clock.System.now()
                .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                .dayOfYear
            return ReframeViewModel.dailyQuotes[dayOfYear % ReframeViewModel.dailyQuotes.size]
        }

    val currentThinkingPhrase: String
        get() = ReframeViewModel.thinkingPhrases[thinkingPhraseIndex % ReframeViewModel.thinkingPhrases.size]

    val isDeepReasoningModel: Boolean
        get() {
            val id = selectedModelName.lowercase()
            return id.contains("reasoner")
                || id.startsWith("o1") || id.startsWith("o3") || id.startsWith("o4")
                || id.contains("reason")
                || id.contains("thinking")
        }

    val isGeminiProModel: Boolean
        get() {
            val id = selectedModelName.lowercase()
            return id.contains("gemini") && id.contains("pro")
        }

    val loadingBannerStyle: LoadingBannerStyle
        get() = when {
            isDeepReasoningModel -> LoadingBannerStyle.DEEP_REASONING
            isGeminiProModel -> LoadingBannerStyle.GEMINI_PRO
            else -> LoadingBannerStyle.NONE
        }

    val homeStage: HomeStage
        get() = when {
            inputText.isBlank() -> HomeStage.QuickStart
            selectedTemplate == null -> HomeStage.WritingThought
            selectedMood.isBlank() -> HomeStage.ChoosingMood
            else -> HomeStage.ReviewReady
        }
}

enum class LoadingBannerStyle { NONE, DEEP_REASONING, GEMINI_PRO }

class ReframeViewModel(
    private val useCase: ReframeUseCase,
    private val streakService: StreakService,
    private val settingsManager: SettingsManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val _uiState = MutableStateFlow(ReframeUiState())
    val uiState: StateFlow<ReframeUiState> = _uiState.asStateFlow()

    private var thinkingTickerJob: Job? = null
    private var retryNoticeJob: Job? = null

    companion object {
        val quickStartPrompts: List<Pair<String, String>> = listOf(
            "💭" to "我觉得自己什么都做不好",
            "😰" to "明天开会我一定会搞砸",
            "😔" to "没有人真正关心我",
            "😤" to "所有事情都不顺利",
            "🫠" to "我太累了，什么都不想做",
        )

        val thinkingPhrases: List<String> = listOf(
            "理解中",
            "梳理中",
            "提炼中",
            "整理回复",
        )

        val dailyQuotes: List<String> = listOf(
            "每一个想法都只是想法，不是事实。",
            "你不需要相信脑海中的每一句话。",
            "今天也在努力理解自己，这已经很了不起了。",
            "改变从觉察开始。",
            "对自己温柔一点，你正在做一件勇敢的事。",
            "情绪像天气，会变的。",
            "你可以感受到痛苦，同时选择前行。",
            "你不需要完美，只需要前进一小步。",
            "承认情绪本身就是一种力量。",
            "慢一点没关系，你已经在路上了。",
        )
    }

    init {
        val provider = settingsManager.getSelectedProvider()
        val modelName = settingsManager.getSelectedModelId().ifEmpty {
            provider.defaultModelId()
        }
        _uiState.value = _uiState.value.copy(
            selectedProvider = provider,
            selectedModelName = modelName,
        )
        loadStreak()
        val count = streakService.todayAnalysisCount()
        _uiState.value = _uiState.value.copy(todayAnalysisCount = count)
    }

    fun clear() {
        scope.cancel()
    }

    // ── Input state mutations ──────────────────────────────────────────

    fun setInputText(text: String) {
        val template = ThinkingTemplate.suggest(text)
        _uiState.value = _uiState.value.copy(
            inputText = text,
            suggestedThinkingTemplate = template,
        )
    }

    fun setSelectedMood(mood: String) {
        _uiState.value = _uiState.value.copy(selectedMood = mood)
    }

    fun setAkathisia(value: Boolean) {
        _uiState.value = _uiState.value.copy(isAkathisia = value)
    }

    fun setSelectedTemplate(template: ThinkingTemplate) {
        _uiState.value = _uiState.value.copy(selectedTemplate = template)
    }

    fun refreshProviderAndModel() {
        val provider = settingsManager.getSelectedProvider()
        val modelName = settingsManager.getSelectedModelId().ifEmpty {
            provider.defaultModelId()
        }
        _uiState.value = _uiState.value.copy(
            selectedProvider = provider,
            selectedModelName = modelName,
        )
    }

    // ── Analyze ────────────────────────────────────────────────────────

    fun analyzeThought(globalSettings: GlobalSettings = GlobalSettings.Default) {
        val thought = _uiState.value.inputText.trim()
        if (thought.isEmpty()) return

        val mood = _uiState.value.selectedMood.trim()
        if (mood.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "先点一个最接近现在状态的心情，再继续。"
            )
            return
        }

        val isAkathisia = _uiState.value.isAkathisia
        val provider = _uiState.value.selectedProvider
        val modelName = _uiState.value.selectedModelName

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            isStreamingResult = true,
            streamingText = "",
            showCrisisBanner = false,
        )

        val isDeepReasoning = _uiState.value.isDeepReasoningModel
        if (isDeepReasoning) startThinkingProgress()

        scope.launch {
            try {
                val output = useCase.streamAnalyze(
                    thought = thought,
                    mood = mood,
                    hasAkathisia = isAkathisia,
                    provider = provider,
                    modelName = modelName,
                    settings = globalSettings,
                )

                if (output.showCrisisBanner) {
                    _uiState.value = _uiState.value.copy(showCrisisBanner = true)
                }

                var currentText = ""
                output.stream.collect { chunk ->
                    currentText += chunk
                    
                    // Simple hack to hide JSON structure from the raw stream 
                    // until we properly implement markdown-streaming prompt
                    val cleaned = cleanStreamContent(currentText)
                    _uiState.value = _uiState.value.copy(streamingText = cleaned)
                }

                // Wait, parseReframeOutput is already used inside the use case 
                // so we can just parse it here again or we could have returned it 
                // from the use case. But we don't have the final parsed result easily
                // returned from streamAnalyze unless we change the signature again.
                // For MVP: parse the full collected JSON here again to display the final result.
                val finalParsed = try {
                    parseReframeOutput(currentText, output.strategy).normalized(globalSettings.thinkingTemplate)
                } catch(e: Exception) {
                    AnalysisResult(
                        distortion = "分析结束",
                        alternative = _uiState.value.streamingText,
                        action = ""
                    )
                }

                _uiState.value = _uiState.value.copy(
                    result = finalParsed,
                    isStreamingResult = false,
                    latestHistoryEntryID = output.historyEntryID,
                )

                // No need to check recoveredByRetry since streaming bypasses Validation client
                
                markStreakToday()
                incrementTodayCount()

            } catch (e: CancellationException) {
                // ignore
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "分析失败，请稍后重试",
                )
            } finally {
                stopThinkingProgress()
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun reset() {
        thinkingTickerJob?.cancel()
        thinkingTickerJob = null
        retryNoticeJob?.cancel()
        retryNoticeJob = null
        stopThinkingProgress()

        _uiState.value = _uiState.value.copy(
            inputText = "",
            selectedMood = "",
            isAkathisia = false,
            result = null,
            errorMessage = null,
            showCrisisBanner = false,
            retryRecoveryNotice = null,
            isStreamingResult = false,
            streamingText = "",
            latestHistoryEntryID = null,
        )
    }

    // ── External prompt (for copy-to-clipboard) ────────────────────────

    fun buildExternalManualPromptText(): String? {
        val thought = _uiState.value.inputText.trim()
        if (thought.isEmpty()) return null
        val mood = _uiState.value.selectedMood.trim()
        if (mood.isEmpty()) return null

        val riskLevel = detectRiskLevel(thought)
        val strategy = routeStrategy(riskLevel)

        return PromptBuilder.buildSystemPrompt(
            template = GlobalSettings.Default.thinkingTemplate,
            strategy = strategy,
            depth = GlobalSettings.Default.analysisDepth,
            style = GlobalSettings.Default.responseStyle,
            mood = mood,
            hasAkathisia = _uiState.value.isAkathisia,
        ) + "\n\n" + PromptBuilder.buildUserPrompt(thought, mood, _uiState.value.isAkathisia)
    }

    // ── Streak helpers ─────────────────────────────────────────────────

    private fun loadStreak() {
        val streak = streakService.loadStreak()
        _uiState.value = _uiState.value.copy(
            currentStreak = streak.current,
            longestStreak = streak.longest,
        )
    }

    private fun markStreakToday() {
        val streak = streakService.markToday()
        _uiState.value = _uiState.value.copy(
            currentStreak = streak.current,
            longestStreak = streak.longest,
        )
    }

    private fun incrementTodayCount() {
        val count = streakService.incrementTodayCount()
        _uiState.value = _uiState.value.copy(todayAnalysisCount = count)
    }

    // ── Thinking progress ticker ───────────────────────────────────────

    private fun startThinkingProgress() {
        _uiState.value = _uiState.value.copy(analysisElapsedSeconds = 0, thinkingPhraseIndex = 0)
        thinkingTickerJob?.cancel()
        thinkingTickerJob = scope.launch {
            var ticks = 0
            while (isActive && _uiState.value.isLoading) {
                delay(1000)
                if (!isActive || !_uiState.value.isLoading) break
                ticks++
                _uiState.value = _uiState.value.copy(
                    analysisElapsedSeconds = ticks,
                    thinkingPhraseIndex = if (ticks % 2 == 0)
                        (_uiState.value.thinkingPhraseIndex + 1) % thinkingPhrases.size
                    else _uiState.value.thinkingPhraseIndex,
                )
            }
        }
    }

    private fun stopThinkingProgress() {
        thinkingTickerJob?.cancel()
        thinkingTickerJob = null
        _uiState.value = _uiState.value.copy(analysisElapsedSeconds = 0, thinkingPhraseIndex = 0)
    }

    // ── Retry recovery notice ──────────────────────────────────────────

    private fun showRetryRecoveryNotice() {
        val notice = "网络波动，已自动重试并成功"
        _uiState.value = _uiState.value.copy(retryRecoveryNotice = notice)
        retryNoticeJob?.cancel()
        retryNoticeJob = scope.launch {
            delay(3500)
            if (isActive && _uiState.value.retryRecoveryNotice == notice) {
                _uiState.value = _uiState.value.copy(retryRecoveryNotice = null)
            }
        }
    }
}

// ── ThinkingTemplate.suggest extension ─────────────────────────────────────

fun ThinkingTemplate.Companion.suggest(text: String): ThinkingTemplate? {
    val lower = text.trim().lowercase()
    return when {
        lower.length <= 5 -> null
        lower.contains("为什么") || lower.contains("怎么会") || lower.contains("怎么办")
            -> ThinkingTemplate.socratic
        lower.contains("做") || lower.contains("去") || lower.contains("行动") || lower.contains("试试")
            -> ThinkingTemplate.behavioral
        lower.contains("觉得") || lower.contains("认为") || lower.contains("感觉") || lower.contains("想")
            -> ThinkingTemplate.cbt
        else -> null
    }
}

// ── Stream Content Cleaner ──────────────────────────────────────────────────

fun cleanStreamContent(currentText: String): String {
    return currentText
        .replace(Regex("\"?[a-zA-Z_]+\"\\s*:\\s*\"?"), "")
        .replace(Regex("[{}\\[\\]]"), "")
        .replace("\\n", "\n")
        .replace("\\\"", "\"")
        .trim()
}
