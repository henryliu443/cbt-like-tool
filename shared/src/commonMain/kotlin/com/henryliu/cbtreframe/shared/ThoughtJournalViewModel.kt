package com.henryliu.cbtreframe.shared

import com.benasher44.uuid.uuid4
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

data class ThoughtJournalUiState(
    val quickInput: String = "",
    val situation: String = "",
    val selectedEmotion: String = "",
    val intensity: Double = 5.0,
    val beliefBefore: Double = 50.0,
    val evidenceFor: String = "",
    val evidenceAgainst: String = "",
    val showAddSheet: Boolean = false,
    val isAnalyzing: Boolean = false,
    val patternReport: ThoughtPatternReport? = null,
    val errorMessage: String? = null,
    val entries: List<ThoughtEntry> = emptyList(),
)

class ThoughtJournalViewModel(
    private val settingsManager: SettingsManager,
    private val repository: ThoughtJournalRepository,
    private val reframeUseCase: ReframeUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val _uiState = MutableStateFlow(ThoughtJournalUiState())
    val uiState: StateFlow<ThoughtJournalUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            repository.getThoughts().collect { entries ->
                _uiState.value = _uiState.value.copy(entries = entries)
            }
        }
    }

    fun clear() {
        scope.cancel()
    }

    // ── Input mutations ────────────────────────────────────────────────

    fun setQuickInput(text: String) {
        _uiState.value = _uiState.value.copy(quickInput = text)
    }

    fun setSituation(text: String) {
        _uiState.value = _uiState.value.copy(situation = text)
    }

    fun setSelectedEmotion(emotion: String) {
        _uiState.value = _uiState.value.copy(selectedEmotion = emotion)
    }

    fun setIntensity(value: Double) {
        _uiState.value = _uiState.value.copy(intensity = value)
    }

    fun setBeliefBefore(value: Double) {
        _uiState.value = _uiState.value.copy(beliefBefore = value)
    }

    fun setEvidenceFor(text: String) {
        _uiState.value = _uiState.value.copy(evidenceFor = text)
    }

    fun setEvidenceAgainst(text: String) {
        _uiState.value = _uiState.value.copy(evidenceAgainst = text)
    }

    fun setShowAddSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddSheet = show)
    }

    // ── Quick capture ──────────────────────────────────────────────────

    fun saveQuickCaptureEntry() {
        val text = _uiState.value.quickInput.trim()
        if (text.isEmpty()) return

        val entry = ThoughtEntry(
            id = uuid4().toString(),
            content = text,
            situation = _uiState.value.situation,
            emotion = _uiState.value.selectedEmotion,
            intensity = _uiState.value.intensity.toInt(),
            beliefBefore = _uiState.value.beliefBefore.toInt(),
            beliefAfter = (_uiState.value.beliefBefore.toInt() - 20).coerceAtLeast(10),
            evidenceFor = _uiState.value.evidenceFor,
            evidenceAgainst = _uiState.value.evidenceAgainst,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )

        repository.addThought(entry)
        resetForm()
    }

    fun updateThought(entry: ThoughtEntry) {
        repository.updateThought(entry)
    }

    fun deleteThought(id: String) {
        repository.deleteThought(id)
    }

    private fun resetForm() {
        _uiState.value = _uiState.value.copy(
            quickInput = "",
            situation = "",
            selectedEmotion = "",
            intensity = 5.0,
            beliefBefore = 50.0,
            evidenceFor = "",
            evidenceAgainst = "",
            showAddSheet = false,
        )
    }

    // ── Pattern analysis ───────────────────────────────────────────────

    fun analyzePatterns() {
        val allEntries = _uiState.value.entries
        val unprocessed = allEntries.filter { !it.isProcessed }
        if (unprocessed.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "没有待整理的想法")
            return
        }

        _uiState.value = _uiState.value.copy(isAnalyzing = true, errorMessage = null)

        scope.launch {
            try {
                val report = reframeUseCase.analyzePatterns(
                    thoughts = unprocessed,
                    provider = settingsManager.getSelectedProvider(),
                    modelName = settingsManager.getSelectedModelId()
                )
                _uiState.value = _uiState.value.copy(
                    patternReport = report,
                    isAnalyzing = false,
                )
                applyAnalysisToEntries(unprocessed, report)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "分析失败：${e.message}",
                    isAnalyzing = false,
                )
            }
        }
    }

    private fun applyAnalysisToEntries(
        entries: List<ThoughtEntry>,
        report: ThoughtPatternReport,
    ) {
        val topDistortion = report.topDistortions.firstOrNull()?.name ?: ""
        val suggestion = report.suggestion

        entries.forEach { entry ->
            val updated = entry.copy(
                isProcessed = true,
                distortionTag = topDistortion,
                balancedThought = suggestion,
                beliefAfter = (entry.beliefBefore - 20).coerceAtLeast(10),
            )
            repository.updateThought(updated)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
