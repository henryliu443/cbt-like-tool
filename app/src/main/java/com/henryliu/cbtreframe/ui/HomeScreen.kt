package com.henryliu.cbtreframe.ui

import androidx.compose.animation.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import com.henryliu.cbtreframe.shared.*

// ──────────────────────────────────────────────────
// HomeScreen — iOS-inspired CBT Reframe UI
// Flow: InputStep → LoadingStep → ResultStep
// Hoisted HomeUiState with callbacks.
// ──────────────────────────────────────────────────

// ── Home Flow Step ────────────────────────────────

enum class HomeFlowStep {
    INPUT,
    LOADING,
    RESULT,
}

// ── Mood Emoji config ─────────────────────────────

data class MoodOption(
    val emoji: String,
    val label: String,
)

val MOOD_OPTIONS = listOf(
    MoodOption("😔", "低落"),
    MoodOption("😰", "焦虑"),
    MoodOption("😤", "愤怒"),
    MoodOption("😟", "担忧"),
    MoodOption("😞", "失望"),
    MoodOption("🫠", "疲惫"),
    MoodOption("😶", "麻木"),
    MoodOption("😣", "内在不安"),
    MoodOption("🥳", "开心"),
    MoodOption("😆", "愉快"),
)

// ── Hoisted HomeUiState ───────────────────────────

enum class InputFlowStep { WRITE_THOUGHT, CHOOSE_MODE, CHOOSE_MOOD }

data class HomeUiState(
    // Input
    val inputText: String = "",
    val selectedMoodLabel: String = "",
    val isAkathisia: Boolean = false,
    val selectedTemplate: ThinkingTemplate = ThinkingTemplate.cbt,
    // Flow
    val currentStep: HomeFlowStep = HomeFlowStep.INPUT,
    val inputFlowStep: InputFlowStep = InputFlowStep.WRITE_THOUGHT,
    // Loading
    val analysisElapsedSeconds: Int = 0,
    val thinkingPhraseIndex: Int = 0,
    val isStreamingResult: Boolean = false,
    val streamingText: String = "",
    val deepReasoningActive: Boolean = false,
    val geminiProActive: Boolean = false,
    // Result
    val result: AnalysisResult? = null,
    val errorMessage: String? = null,
    val showCrisisBanner: Boolean = false,
    val retryRecoveryNotice: String? = null,
    val latestHistoryEntryID: String? = null,
    // Stats
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val todayAnalysisCount: Int = 0,
    // Greeting
    val greeting: String = "早上好",
    val todayQuote: String = "",
    val suggestedTemplate: ThinkingTemplate? = null,
    // Quick Start prompts
    val quickStartPrompts: List<Pair<String, String>> = emptyList(),
    // Thinking phrases for loading
    val thinkingPhrases: List<String> = emptyList(),
    // Secondary tools collapsed state
    val showSecondaryTools: Boolean = false,
    // External prompt (for clipboard)
    val externalPromptText: String? = null,
)

// ── Callbacks that HomeScreen exposes ─────────────

class HomeScreenCallbacks(
    val onInputTextChanged: (String) -> Unit,
    val onMoodSelected: (String) -> Unit,
    val onAkathisiaToggled: (Boolean) -> Unit,
    val onTemplateSelected: (ThinkingTemplate) -> Unit,
    val onAnalyzeClicked: () -> Unit,
    val onResetClicked: () -> Unit,
    val onSecondaryToolsToggled: (Boolean) -> Unit,
    val onRetryClicked: () -> Unit,
    val onCopyPromptToClipboard: () -> Unit,
)

// ═══════════════════════════════════════════════════
// HOMESCREEN — Root Composable
// ═══════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ReframeViewModel,
    globalSettings: GlobalSettings = GlobalSettings.Default,
    onGlobalSettingsChange: (GlobalSettings) -> Unit = {},
) {
    val vmState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var isInputFocused by remember { mutableStateOf(false) }
    var inputFlowStep by remember { mutableStateOf(InputFlowStep.WRITE_THOUGHT) }

    // ── Mapping ViewModel state → HomeUiState ──
    val uiState = remember(vmState, globalSettings) {
        HomeUiState(
            inputText = vmState.inputText,
            selectedMoodLabel = vmState.selectedMood,
            isAkathisia = vmState.isAkathisia,
            selectedTemplate = globalSettings.thinkingTemplate,
            currentStep = when {
                vmState.isLoading -> HomeFlowStep.LOADING
                vmState.result != null && !vmState.isStreamingResult -> HomeFlowStep.RESULT
                else -> HomeFlowStep.INPUT
            },
            analysisElapsedSeconds = vmState.analysisElapsedSeconds,
            thinkingPhraseIndex = vmState.thinkingPhraseIndex,
            isStreamingResult = vmState.isStreamingResult,
            streamingText = vmState.streamingText,
            deepReasoningActive = vmState.loadingBannerStyle == LoadingBannerStyle.DEEP_REASONING,
            geminiProActive = vmState.loadingBannerStyle == LoadingBannerStyle.GEMINI_PRO,
            result = vmState.result,
            errorMessage = vmState.errorMessage,
            showCrisisBanner = vmState.showCrisisBanner,
            retryRecoveryNotice = vmState.retryRecoveryNotice,
            latestHistoryEntryID = vmState.latestHistoryEntryID,
            currentStreak = vmState.currentStreak,
            longestStreak = vmState.longestStreak,
            todayAnalysisCount = vmState.todayAnalysisCount,
            greeting = vmState.greeting,
            todayQuote = vmState.todayQuote,
            suggestedTemplate = ThinkingTemplate.suggest(vmState.inputText),
            quickStartPrompts = ReframeViewModel.quickStartPrompts,
            thinkingPhrases = ReframeViewModel.thinkingPhrases,
        )
    }

    var showSecondaryTools by remember { mutableStateOf(false) }

    val callbacks = remember(viewModel, globalSettings, onGlobalSettingsChange) {
        HomeScreenCallbacks(
            onInputTextChanged = { viewModel.setInputText(it) },
            onMoodSelected = { viewModel.setSelectedMood(it) },
            onAkathisiaToggled = { viewModel.setAkathisia(it) },
            onTemplateSelected = { tpl ->
                onGlobalSettingsChange(globalSettings.copy(thinkingTemplate = tpl))
            },
            onAnalyzeClicked = { viewModel.analyzeThought(globalSettings) },
            onResetClicked = { viewModel.reset() },
            onSecondaryToolsToggled = { showSecondaryTools = it },
            onRetryClicked = { viewModel.analyzeThought(globalSettings) },
            onCopyPromptToClipboard = {
                val prompt = viewModel.buildExternalManualPromptText()
                // The actual clipboard copy is handled by the caller
            },
        )
    }

    // Atmospheric gradient
    val gradientColors = remember {
        listOf(
            Color(0xFFFFF3E0), // warm cream
            Color(0xFFFFECB3), // soft amber
            Color(0xFFFFF8E1), // light lemon
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AtmosphericBackground(gradientColors = gradientColors)

        when (uiState.currentStep) {
            HomeFlowStep.INPUT -> {
                InputStep(
                    uiState = uiState.copy(showSecondaryTools = showSecondaryTools),
                    callbacks = callbacks,
                    isInputFocused = isInputFocused,
                    onFocusChanged = { isInputFocused = it },
                    focusManager = focusManager,
                    inputFlowStep = inputFlowStep,
                    onInputFlowStepChanged = { inputFlowStep = it },
                )
            }
            HomeFlowStep.LOADING -> {
                LoadingStep(
                    uiState = uiState,
                    callbacks = callbacks,
                )
            }
            HomeFlowStep.RESULT -> {
                ResultStep(
                    uiState = uiState,
                    callbacks = callbacks,
                    focusManager = focusManager,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// INPUT STEP
// ═══════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputStep(
    uiState: HomeUiState,
    callbacks: HomeScreenCallbacks,
    isInputFocused: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    inputFlowStep: InputFlowStep,
    onInputFlowStepChanged: (InputFlowStep) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // ── Greeting ──
            AtmosphericGreeting(uiState = uiState)

            // ── Dashboard ──
            AsymmetricDashboard(uiState = uiState)

            // ── Quick Start prompts ──
            QuickStartSection(
                prompts = uiState.quickStartPrompts,
                onPromptSelected = { text ->
                    callbacks.onInputTextChanged(text)
                }
            )

            // ── Mood Picker ──
            AnimatedVisibility(
                visible = inputFlowStep >= InputFlowStep.CHOOSE_MOOD,
                enter = expandVertically() + fadeIn()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    MoodPickerSection(
                        selectedMoodLabel = uiState.selectedMoodLabel,
                        onMoodSelected = callbacks.onMoodSelected,
                    )
                    AkathisiaToggle(
                        isChecked = uiState.isAkathisia,
                        onCheckedChange = callbacks.onAkathisiaToggled,
                    )
                }
            }

            // ── Template Picker ──
            AnimatedVisibility(
                visible = inputFlowStep >= InputFlowStep.CHOOSE_MODE,
                enter = expandVertically() + fadeIn()
            ) {
                TemplatePickerSection(
                    selectedTemplate = uiState.selectedTemplate,
                    suggestedTemplate = uiState.suggestedTemplate,
                    onTemplateSelected = {
                        callbacks.onTemplateSelected(it)
                        onInputFlowStepChanged(InputFlowStep.CHOOSE_MOOD)
                    },
                )
            }

            // ── Thought Input ──
            ThoughtInputCard(
                inputText = uiState.inputText,
                onInputChanged = { 
                    callbacks.onInputTextChanged(it)
                    if (it.isNotBlank() && inputFlowStep == InputFlowStep.WRITE_THOUGHT) {
                        onInputFlowStepChanged(InputFlowStep.CHOOSE_MODE)
                    }
                },
                isFocused = isInputFocused,
                onFocusChanged = onFocusChanged,
            )

            // ── Analyze Button ──
            DynamicAnalyzeButton(
                inputFlowStep = inputFlowStep,
                canAdvanceFromThought = uiState.inputText.isNotBlank(),
                canSubmitAnalysis = uiState.inputText.isNotBlank() && uiState.selectedMoodLabel.isNotBlank(),
                onAdvanceStep = { onInputFlowStepChanged(it) },
                onSubmit = {
                    onFocusChanged(false)
                    callbacks.onAnalyzeClicked()
                }
            )

            // ── Secondary Tools (collapsible) ──
            AnimatedVisibility(
                visible = inputFlowStep >= InputFlowStep.CHOOSE_MODE,
                enter = expandVertically() + fadeIn()
            ) {
                SecondaryToolsSection(
                    expanded = uiState.showSecondaryTools,
                    onToggle = callbacks.onSecondaryToolsToggled,
                    externalPromptText = uiState.externalPromptText,
                    onCopyPromptToClipboard = callbacks.onCopyPromptToClipboard,
                )
            }

            // ── Safety Banner ──
            SafetyBannerView()

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ═══════════════════════════════════════════════════
// LOADING STEP
// ═══════════════════════════════════════════════════

@Composable
private fun LoadingStep(
    uiState: HomeUiState,
    callbacks: HomeScreenCallbacks,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .widthIn(max = 400.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // ── Model-aware banner ──
            when {
                uiState.deepReasoningActive -> {
                    DeepReasoningBanner()
                }
                uiState.geminiProActive -> {
                    GeminiProBanner()
                }
            }

            // ── Animated spinner ──
            LoadingSpinner()

            // ── Thinking phrase ──
            val phrase = uiState.thinkingPhrases.getOrElse(
                uiState.thinkingPhraseIndex % uiState.thinkingPhrases.size
            ) { "处理中" }
            Text(
                phrase,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )

            // ── Elapsed time ──
            if (uiState.analysisElapsedSeconds > 0) {
                Text(
                    "已用时 ${uiState.analysisElapsedSeconds}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── Streaming result card ──
            if (uiState.isStreamingResult) {
                Spacer(modifier = Modifier.height(8.dp))
                StreamingResultCard(text = uiState.streamingText)
            }

            // ── Error banner ──
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ErrorBanner(error = uiState.errorMessage)
            }

            // ── Crisis banner ──
            if (uiState.showCrisisBanner) {
                Spacer(modifier = Modifier.height(8.dp))
                SafetyBannerView()
            }

            // ── Retry ──
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = callbacks.onRetryClicked,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("重试")
                }
            }

            // ── Cancel ──
            if (!uiState.isStreamingResult) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = callbacks.onResetClicked) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// ═══════════════════════════════════════════════════
// RESULT STEP
// ═══════════════════════════════════════════════════

@Composable
private fun ResultStep(
    uiState: HomeUiState,
    callbacks: HomeScreenCallbacks,
    focusManager: androidx.compose.ui.focus.FocusManager,
) {
    val result = uiState.result ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // ── Greeting ──
            AtmosphericGreeting(uiState = uiState)

            // ── Result Card (full result) ──
            ResultCard(result = result)

            // ── Crisis banner if applicable ──
            if (uiState.showCrisisBanner) {
                SafetyBannerView()
            }

            // ── Error banner ──
            if (uiState.errorMessage != null) {
                ErrorBanner(error = uiState.errorMessage)
            }

            // ── Retry recovery notice ──
            if (uiState.retryRecoveryNotice != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            uiState.retryRecoveryNotice,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // ── New analysis button ──
            OutlinedButton(
                onClick = callbacks.onResetClicked,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("开始新的分析")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ═══════════════════════════════════════════════════
// MOOD PICKER
// ═══════════════════════════════════════════════════

@Composable
private fun MoodPickerSection(
    selectedMoodLabel: String,
    onMoodSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "今天的感受",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            "选择一个最贴近你当下心情的标签",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MOOD_OPTIONS.take(5)) { mood ->
                val isSelected = selectedMoodLabel == mood.label
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onMoodSelected(if (isSelected) "" else mood.label)
                    },
                    label = {
                        Text(
                            "${mood.emoji} ${mood.label}",
                            fontSize = 13.sp,
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// AKATHISIA TOGGLE
// ═══════════════════════════════════════════════════

@Composable
private fun AkathisiaToggle(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🦵", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "静坐不能 (Akathisia)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "内心不安、无法静坐的感受",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

// ═══════════════════════════════════════════════════
// TEMPLATE PICKER
// ═══════════════════════════════════════════════════

@Composable
private fun TemplatePickerSection(
    selectedTemplate: ThinkingTemplate,
    suggestedTemplate: ThinkingTemplate?,
    onTemplateSelected: (ThinkingTemplate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "思考模板",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ThinkingTemplate.entries.toList()) { template ->
                val isSelected = template == selectedTemplate
                val isSuggested = template == suggestedTemplate
                val (label, desc) = templateDisplayInfo(template)

                FilterChip(
                    selected = isSelected,
                    onClick = { onTemplateSelected(template) },
                    label = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                                if (isSuggested) {
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "推荐",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            Text(
                                desc,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
    }
}

private fun templateDisplayInfo(template: ThinkingTemplate): Pair<String, String> = when (template) {
    ThinkingTemplate.cbt -> "CBT 重构" to "识别与转换负面想法"
    ThinkingTemplate.socratic -> "苏格拉底式" to "提问探索深层信念"
    ThinkingTemplate.behavioral -> "行为激活" to "小步骤促进行动改变"
}

// ═══════════════════════════════════════════════════
// THOUGHT INPUT CARD
// ═══════════════════════════════════════════════════

@Composable
private fun ThoughtInputCard(
    inputText: String,
    onInputChanged: (String) -> Unit,
    isFocused: Boolean,
    onFocusChanged: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 8.dp else 2.dp
        ),
        border = if (isFocused) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha=0.45f)) 
                 else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "写下你的想法",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
                    .onFocusChanged { onFocusChanged(it.isFocused) },
                placeholder = {
                    Text(
                        "描述让你感到困扰的情境、想法或感受…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onFocusChanged(false) }
                ),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${inputText.length} 字",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

// ═══════════════════════════════════════════════════
// DYNAMIC ANALYZE BUTTON
// ═══════════════════════════════════════════════════

@Composable
private fun DynamicAnalyzeButton(
    inputFlowStep: InputFlowStep,
    canAdvanceFromThought: Boolean,
    canSubmitAnalysis: Boolean,
    onAdvanceStep: (InputFlowStep) -> Unit,
    onSubmit: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientOffset"
    )

    val buttonText = when (inputFlowStep) {
        InputFlowStep.WRITE_THOUGHT -> "下一步：选最省力的方式"
        InputFlowStep.CHOOSE_MODE -> "下一步：点当前心情"
        InputFlowStep.CHOOSE_MOOD -> "开始分析"
    }

    val isEnabled = when (inputFlowStep) {
        InputFlowStep.WRITE_THOUGHT, InputFlowStep.CHOOSE_MODE -> canAdvanceFromThought
        InputFlowStep.CHOOSE_MOOD -> canSubmitAnalysis
    }

    val onClick = {
        if (inputFlowStep == InputFlowStep.WRITE_THOUGHT) {
            onAdvanceStep(InputFlowStep.CHOOSE_MODE)
        } else if (inputFlowStep == InputFlowStep.CHOOSE_MODE) {
            onAdvanceStep(InputFlowStep.CHOOSE_MOOD)
        } else {
            onSubmit()
        }
    }

    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent, // Managed by background modifier
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        ),
        contentPadding = PaddingValues(0.dp) // Ensure background fills exactly
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isEnabled) {
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6B4EE6), // Gradient Start
                                Color(0xFF9B6FE6)  // Gradient End
                            )
                        )
                    } else {
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.Transparent)
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (inputFlowStep == InputFlowStep.CHOOSE_MOOD) Icons.Default.AutoAwesome else Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = if (isEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    buttonText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// SECONDARY TOOLS (Collapsible Disclosure Group)
// ═══════════════════════════════════════════════════

@Composable
private fun SecondaryToolsSection(
    expanded: Boolean,
    onToggle: (Boolean) -> Unit,
    externalPromptText: String?,
    onCopyPromptToClipboard: () -> Unit,
) {
    Column {
        // ── Disclosure header ──
        TextButton(
            onClick = { onToggle(!expanded) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.MoreHoriz,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "更多选项",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }

        // ── Expandable content ──
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // ── Copy prompt to clipboard ──
                if (externalPromptText != null) {
                    OutlinedButton(
                        onClick = onCopyPromptToClipboard,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("复制完整 Prompt", fontSize = 13.sp)
                    }
                }

                // Future tools can be placed here
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "更多辅助工具即将推出",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// LOADING SPINNER
// ═══════════════════════════════════════════════════

@Composable
private fun LoadingSpinner() {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
        ),
        label = "rotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier.size(80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(60.dp).rotate(rotation)) {
            val strokeWidth = 4.dp.toPx()
            val gap = 40f
            drawArc(
                color = primaryColor,
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Text(
            "🧠",
            fontSize = 28.sp,
        )
    }
}

// ═══════════════════════════════════════════════════
// MODEL-AWARE BANNERS
// ═══════════════════════════════════════════════════

@Composable
private fun DeepReasoningBanner() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🧬", fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "深度推理模型",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "正在进行多步推理，可能需要更长时间",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun GeminiProBanner() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("✨", fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "Gemini Pro",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "使用 Gemini Pro 模型进行分析",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}


// ═══════════════════════════════════════════════════
// ATMOSPHERIC GREETING
// ═══════════════════════════════════════════════════

@Composable
private fun AtmosphericGreeting(uiState: HomeUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(
            uiState.greeting,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            uiState.todayQuote,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp,
        )
    }
}

// ═══════════════════════════════════════════════════
// ASYMMETRIC DASHBOARD
// ═══════════════════════════════════════════════════

@Composable
private fun AsymmetricDashboard(uiState: HomeUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(0.6f),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ── Streak card (larger) ──
        Card(
            modifier = Modifier.weight(1.2f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🔥", fontSize = 24.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "连续打卡",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "${uiState.currentStreak} 天",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "最长 ${uiState.longestStreak} 天",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
        }

        // ── Today count card (smaller) ──
        Card(
            modifier = Modifier.weight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📊", fontSize = 24.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "今日分析",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "${uiState.todayAnalysisCount} 次",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// QUICK START SECTION
// ═══════════════════════════════════════════════════

@Composable
private fun QuickStartSection(
    prompts: List<Pair<String, String>>,
    onPromptSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "快速开始",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(prompts) { (emoji, text) ->
                Card(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 140.dp)
                        .clickable { onPromptSelected(text) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(emoji, fontSize = 18.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// RESULT CARD
// ═══════════════════════════════════════════════════

@Composable
private fun ResultCard(result: AnalysisResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // ── Header ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "分析结果",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Cognitive Distortion ──
            if (result.distortion.isNotBlank()) {
                Text(
                    "认知扭曲",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp),
                        )
                        .padding(10.dp),
                ) {
                    Text("🔹", fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        result.distortion,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── State Assessment (behavioral) ──
            val stateAssessment = result.stateAssessment
            if (!stateAssessment.isNullOrBlank()) {
                Text(
                    "状态评估",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stateAssessment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
            }

            // ── Alternative Thought ──
            if (result.alternative.isNotBlank()) {
                Text(
                    "替代想法",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    ),
                ) {
                    Text(
                        result.alternative,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Socratic Questions ──
            val questions = result.questions
            if (!questions.isNullOrEmpty()) {
                Text(
                    "引导问题",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                questions.forEach { q ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                    ) {
                        Text("• ", color = MaterialTheme.colorScheme.primary)
                        Text(
                            q,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Action ──
            if (result.action.isNotBlank()) {
                Text(
                    "建议行动",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    result.action,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// STREAMING RESULT CARD
// ═══════════════════════════════════════════════════

@Composable
private fun StreamingResultCard(text: String) {
    var visibleCount by remember(text) { mutableIntStateOf(1) }
    var isPaused by remember { mutableStateOf(false) }
    var showAll by remember { mutableStateOf(false) }

    val chunks = remember(text) {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isNotEmpty()) lines
        else text.split(Regex("[。！？]")).map { it.trim() }.filter { it.isNotEmpty() }
    }

    LaunchedEffect(text, isPaused, showAll) {
        if (showAll) {
            visibleCount = chunks.size
            return@LaunchedEffect
        }
        while (!isPaused && !showAll && visibleCount < chunks.size) {
            kotlinx.coroutines.delay(900)
            visibleCount++
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "分段生成中",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            if (text.isEmpty()) {
                Text(
                    "正在生成…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                chunks.take(if (showAll) chunks.size else visibleCount.coerceIn(1, chunks.size))
                    .forEach { chunk ->
                        val isHighlighted = chunk.contains("替代想法") ||
                            chunk.contains("下一步行动") ||
                            chunk.contains("积极视角")
                        Text(
                            chunk,
                            modifier = if (isHighlighted)
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(10.dp)
                            else Modifier.padding(vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isHighlighted) FontWeight.Medium else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { isPaused = !isPaused },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isPaused) "继续" else "暂停", fontSize = 13.sp)
                }
                Button(
                    onClick = { showAll = !showAll },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (showAll) "分段查看" else "显示全部", fontSize = 13.sp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// SAFETY BANNER
// ═══════════════════════════════════════════════════

@Composable
private fun SafetyBannerView() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "如果你正在经历危机，请立即寻求专业帮助。AI 不能替代专业的心理健康服务。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "📞 心理援助热线：400-161-9995\n📞 北京心理危机干预中心：010-82951332",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                lineHeight = 20.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════
// ERROR BANNER
// ═══════════════════════════════════════════════════

@Composable
private fun ErrorBanner(error: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

// ═══════════════════════════════════════════════════
// ═══════════════════════════════════════════════════
// ATMOSPHERIC BACKGROUND
// ═══════════════════════════════════════════════════

@Composable
private fun AtmosphericBackground(gradientColors: List<Color>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val hueShift = 42f

        // Warm glow in upper-left
        val warmGlow = Brush.radialGradient(
            colors = listOf(
                Color.hsl(hueShift % 360f, 0.5f, 0.96f, 0.7f),
                Color.hsl((hueShift + 30f) % 360f, 0.4f, 0.94f, 0f)
            ),
            center = Offset(w * 0.35f, h * 0.25f),
            radius = maxOf(w, h) * 0.7f
        )
        drawRect(brush = warmGlow)

        // Secondary bloom in bottom-right
        val coolGlow = Brush.radialGradient(
            colors = listOf(
                Color.hsl((hueShift + 180f) % 360f, 0.25f, 0.97f, 0.4f),
                Color.hsl((hueShift + 210f) % 360f, 0.2f, 0.95f, 0f)
            ),
            center = Offset(w * 0.7f, h * 0.6f),
            radius = maxOf(w, h) * 0.5f
        )
        drawRect(brush = coolGlow)

        // Tiny floating orbs for depth
        val orbCount = 6
        for (i in 0 until orbCount) {
            val phase = (hueShift + i * 57f) % 360f
            val ox = w * (0.15f + (i % 3) * 0.30f + sin(phase * 0.017f) * 0.06f)
            val oy = h * (0.15f + (i / 3) * 0.25f + cos(phase * 0.013f) * 0.06f)
            val or = maxOf(w, h) * 0.08f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.hsl((phase + 30f) % 360f, 0.35f, 0.85f, 0.18f),
                        Color.hsl((phase + 30f) % 360f, 0.35f, 0.85f, 0f)
                    ),
                    center = Offset(ox, oy),
                    radius = or
                ),
                radius = or,
                center = Offset(ox, oy)
            )
        }
    }
}
