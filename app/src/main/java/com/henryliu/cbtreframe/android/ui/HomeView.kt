package com.henryliu.cbtreframe.android.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.henryliu.cbtreframe.shared.*
import com.henryliu.cbtreframe.shared.db.HistoryEntity
import kotlin.math.*
import kotlinx.coroutines.delay

// ── Private helpers ──

private enum class HomeFlowStep(val rawValue: Int) {
    WriteThought(0),
    ChooseMode(1),
    ChooseMood(2);
}

private data class MoodTag(val emoji: String, val label: String)

private val sharedMoods = listOf(
    MoodTag("\uD83D\uDE14", "\u4F4E\u843D"),
    MoodTag("\uD83D\uDE30", "\u7126\u8651"),
    MoodTag("\uD83D\uDE24", "\u6124\u6012"),
    MoodTag("\uD83D\uDE1F", "\u62C5\u5FE7"),
    MoodTag("\uD83D\uDE1E", "\u5931\u671B"),
    MoodTag("\uD83E\uDEE0", "\u75B2\u60EB"),
    MoodTag("\uD83D\uDE36", "\u9EBB\u6728"),
    MoodTag("\uD83D\uDE23", "\u5185\u5728\u4E0D\u5B89"),
    MoodTag("\uD83E\uDD73", "\u5F00\u5FC3"),
    MoodTag("\uD83D\uDE06", "\u6109\u5FEB"),
)

// ═══════════════════════════════════════════════════
// HOMEVIEW — faithful replica of HomeView.swift
// ═══════════════════════════════════════════════════

@Composable
fun HomeView(
    viewModel: ReframeViewModel,
    globalSettings: GlobalSettings,
    recentHistory: List<HistoryEntity> = emptyList(),
    onTemplateChanged: (ThinkingTemplate) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current
    val focus = LocalFocusManager.current
    val acc = MaterialTheme.colorScheme.primary

    var flowStep by remember { mutableStateOf(HomeFlowStep.WriteThought) }
    var inputFocused by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var showExternal by remember { mutableStateOf(false) }
    var geminiPulse by remember { mutableStateOf(false) }
    var btnPressed by remember { mutableStateOf(false) }

    val text = uiState.inputText
    val result = uiState.result
    val loading = uiState.isLoading
    val streaming = uiState.isStreamingResult

    // React to inputText changes — SwiftUI .onChange(of: inputText)
    LaunchedEffect(text) {
        if (text.trim().isNotEmpty() && flowStep == HomeFlowStep.WriteThought) {
            flowStep = HomeFlowStep.ChooseMode
        }
    }
    LaunchedEffect(Unit) {
        viewModel.refreshProviderAndModel()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeBackground(acc)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focus.clearFocus() }
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // ── headerSection ──
                HeaderSection(greeting = uiState.greeting, quote = uiState.todayQuote)

                // ── todayDashboard ──
                TodayDashboard(uiState.currentStreak, uiState.todayAnalysisCount, uiState.longestStreak, acc)

                // ── newAnalysisButton (only when result != nil) ──
                if (result != null) {
                    NewAnalysisBtn(onClick = { viewModel.reset(); flowStep = HomeFlowStep.WriteThought })
                }

                // ── Primary flow ──
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    ThoughtInputCard(
                        inputText = text,
                        onInputChanged = { viewModel.setInputText(it) },
                        isFocused = inputFocused,
                        onFocusChanged = { inputFocused = it },
                        accColor = acc
                    )

                    AnimatedVisibility(
                        visible = flowStep.rawValue >= HomeFlowStep.ChooseMode.rawValue,
                        enter = fadeIn() + slideInVertically { -it / 4 },
                        exit = fadeOut() + slideOutVertically { -it / 4 }
                    ) {
                        TemplatePicker(
                            selectedTemplate = globalSettings.thinkingTemplate,
                            suggestedTemplate = ThinkingTemplate.suggest(text),
                            onTemplateSelected = onTemplateChanged,
                            accColor = acc
                        )
                    }

                    AnimatedVisibility(
                        visible = flowStep.rawValue >= HomeFlowStep.ChooseMood.rawValue,
                        enter = fadeIn() + slideInVertically { -it / 4 },
                        exit = fadeOut() + slideOutVertically { -it / 4 }
                    ) {
                        MoodPicker(
                            selectedMood = uiState.selectedMood,
                            isAkathisia = uiState.isAkathisia,
                            onMoodSelected = { viewModel.setSelectedMood(it) },
                            onAkathisiaChanged = { viewModel.setAkathisia(it) },
                            accColor = acc
                        )
                    }

                    AnalyzeBtn(
                        flowStep = flowStep,
                        isLoading = loading,
                        isButtonPressed = btnPressed,
                        canSubmit = {
                            val t = text.trim()
                            when (flowStep) {
                                HomeFlowStep.WriteThought -> t.isNotEmpty()
                                HomeFlowStep.ChooseMode -> t.isNotEmpty()
                                HomeFlowStep.ChooseMood -> t.isNotEmpty() && uiState.selectedMood.trim().isNotEmpty()
                            }
                        },
                        onClick = {
                            focus.clearFocus()
                            when (flowStep) {
                                HomeFlowStep.WriteThought -> flowStep = HomeFlowStep.ChooseMode
                                HomeFlowStep.ChooseMode -> flowStep = HomeFlowStep.ChooseMood
                                HomeFlowStep.ChooseMood -> viewModel.analyzeThought(globalSettings)
                            }
                        },
                        onPressChanged = { btnPressed = it }
                    )
                }

                // ── More options ──
                AnimatedVisibility(
                    visible = flowStep.rawValue >= HomeFlowStep.ChooseMode.rawValue,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                ) {
                    MoreSection(
                        expanded = showMore,
                        onToggle = { showMore = it },
                        accColor = acc,
                        canCopy = {
                            val t = text.trim()
                            t.isNotEmpty() && uiState.selectedMood.trim().isNotEmpty() && !loading
                        },
                        onCopyPrompt = {
                            viewModel.buildExternalManualPromptText()?.let { p ->
                                clipboard.setText(AnnotatedString(p))
                                showExternal = true
                            }
                        }
                    )
                }

                // ── quickStartSection ──
                AnimatedVisibility(
                    visible = text.trim().isEmpty() && result == null,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                ) {
                    QuickStart(
                        prompts = ReframeViewModel.quickStartPrompts,
                        onPromptSelected = { viewModel.setInputText(it) }
                    )
                }

                // ── Banners ──
                AnimatedVisibility(
                    visible = loading && uiState.loadingBannerStyle != LoadingBannerStyle.NONE,
                    enter = fadeIn() + scaleIn(initialScale = 0.95f),
                    exit = fadeOut()
                ) {
                    when (uiState.loadingBannerStyle) {
                        LoadingBannerStyle.DEEP_REASONING -> DeepReasonBanner(
                            elapsedSeconds = uiState.analysisElapsedSeconds,
                            thinkingPhrase = uiState.currentThinkingPhrase,
                            accColor = acc
                        )
                        LoadingBannerStyle.GEMINI_PRO -> GeminiBanner(
                            onAppear = { geminiPulse = true },
                            accColor = acc
                        )
                        else -> {}
                    }
                }

                AnimatedVisibility(
                    visible = uiState.retryRecoveryNotice != null,
                    enter = fadeIn() + slideInVertically { -it / 4 },
                    exit = fadeOut() + slideOutVertically { -it / 4 }
                ) {
                    uiState.retryRecoveryNotice?.let { msg ->
                        RetryBanner(message = msg, accColor = acc)
                    }
                }

                AnimatedVisibility(
                    visible = uiState.showCrisisBanner,
                    enter = fadeIn() + scaleIn(initialScale = 0.95f),
                    exit = fadeOut()
                ) {
                    SafetyBannerView()
                }

                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter = fadeIn() + slideInVertically { -it / 4 },
                    exit = fadeOut() + slideOutVertically { -it / 4 }
                ) {
                    uiState.errorMessage?.let { msg ->
                        ErrorBanner(message = msg, onDismiss = { viewModel.reset() })
                    }
                }

                // ── ResultCardView ──
                AnimatedVisibility(
                    visible = result != null,
                    enter = fadeIn() + slideInVertically { it / 4 } + scaleIn(initialScale = 0.95f),
                    exit = fadeOut()
                ) {
                    result?.let { r ->
                        ResultCardView(
                            result = r,
                            template = globalSettings.thinkingTemplate,
                            inputThought = text,
                            moodTag = uiState.selectedMood,
                            analysisDepthLabel = globalSettings.analysisDepth.name,
                            historyEntryID = uiState.latestHistoryEntryID
                        )
                    }
                }

                // ── StreamingResultView ──
                if (streaming) {
                    StreamingView(text = uiState.streamingText)
                }

                // ── recentHistoryPreview ──
                AnimatedVisibility(
                    visible = result == null && recentHistory.isNotEmpty(),
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                ) {
                    RecentPreview(entries = recentHistory.take(3), accColor = acc)
                }

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    if (showExternal) {
        AlertDialog(
            onDismissRequest = { showExternal = false },
            title = { Text("\u5DF2\u590D\u5236\u5230\u526A\u8D34\u677F") },
            text = { Text("\u5728\u65B0\u5EFA\u5BF9\u8BDD\u91CC\u7C98\u8D34\u521A\u624D\u590D\u5236\u7684\u5168\u90E8\u5185\u5BB9\u5373\u53EF\u3002") },
            confirmButton = {
                TextButton(onClick = { showExternal = false }) { Text("\u5B8C\u6210") }
            }
        )
    }
}

// ═══════════════════════════════════════════════════
// HOME BACKGROUND
// ═══════════════════════════════════════════════════

@Composable
private fun HomeBackground(accColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Color(0xFFF2F2F7))
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    accColor.copy(alpha = 0.07f),
                    Color.Transparent,
                    Color(0xFFF2F2F7),
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height),
            ),
        )
    }
}

// ═══════════════════════════════════════════════════
// HEADER SECTION
// ═══════════════════════════════════════════════════

@Composable
private fun HeaderSection(greeting: String, quote: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                greeting,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                softWrap = true,
            )
            Text(
                quote,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                softWrap = true,
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        AnimatedBrainIcon()
    }
}

@Composable
private fun AnimatedBrainIcon() {
    val tr = rememberInfiniteTransition(label = "brain")
    val scale by tr.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )
    val glow by tr.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )
    Icon(
        imageVector = Icons.Default.Psychology,
        contentDescription = null,
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                alpha = glow,
            ),
        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f + 0.3f * glow),
    )
}

// ═══════════════════════════════════════════════════
// TODAY DASHBOARD
// ═══════════════════════════════════════════════════

@Composable
private fun TodayDashboard(
    currentStreak: Int,
    todayCount: Int,
    longestStreak: Int,
    accColor: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DashItem(Icons.Default.LocalFireDepartment, "$currentStreak", "连续天数", Color(0xFFFF9500))
            DashDivider()
            DashItem(Icons.Default.Psychology, "$todayCount", "今日分析", accColor)
            DashDivider()
            DashItem(Icons.Default.EmojiEvents, "$longestStreak", "最长连续", Color(0xFFFFCC00))
        }
    }
}

@Composable
private fun RowScope.DashItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DashDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
            ),
    )
}

// ═══════════════════════════════════════════════════
// NEW ANALYSIS BUTTON
// ═══════════════════════════════════════════════════

@Composable
private fun NewAnalysisBtn(onClick: () -> Unit) {
    val acc = MaterialTheme.colorScheme.primary
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = acc.copy(alpha = 0.1f),
            contentColor = acc,
        ),
        border = BorderStroke(1.dp, acc.copy(alpha = 0.2f)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "开始新的分析",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
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
    accColor: Color,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) accColor.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = accColor,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "写1句：现在最烦的事",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 128.dp)
                    .onFocusChanged { onFocusChanged(it.isFocused) },
                placeholder = {
                    Text(
                        "例如：我觉得自己做什么都不够好...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                textStyle = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

// ═══════════════════════════════════════════════════
// TEMPLATE PICKER
// ═══════════════════════════════════════════════════

@Composable
private fun TemplatePicker(
    selectedTemplate: ThinkingTemplate,
    suggestedTemplate: ThinkingTemplate?,
    onTemplateSelected: (ThinkingTemplate) -> Unit,
    accColor: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = accColor,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "选最快开始的方式",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (suggestedTemplate != null && suggestedTemplate != selectedTemplate) {
                SuggestionChip(
                    onClick = { onTemplateSelected(suggestedTemplate) },
                    label = {
                        Text(
                            "推荐: ${templateLabel(suggestedTemplate)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                    },
                    icon = {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = accColor,
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = accColor.copy(alpha = 0.12f),
                        labelColor = accColor,
                        iconContentColor = accColor,
                    ),
                    border = null,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ThinkingTemplate.entries.forEach { template ->
                    TemplateCol(
                        template = template,
                        isSelected = selectedTemplate == template,
                        isSuggested = suggestedTemplate == template && selectedTemplate != template,
                        accColor = accColor,
                        onClick = { onTemplateSelected(template) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateCol(
    template: ThinkingTemplate,
    isSelected: Boolean,
    isSuggested: Boolean,
    accColor: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(82.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Canvas(modifier = Modifier.size(56.dp)) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFF6C63FF),
                                Color(0xFF00B4D8),
                                Color(0xFF48CAE4),
                                Color(0xFF6C63FF),
                            ),
                        ),
                        radius = 28.dp.toPx(),
                        style = Stroke(width = 2.5.dp.toPx()),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) accColor.copy(alpha = 0.92f)
                        else accColor.copy(alpha = if (isSuggested) 0.16f else 0.1f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = templateIcon(template),
                    contentDescription = null,
                    tint = if (isSelected) Color.White else accColor,
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        Text(
            templateLabel(template),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )

        if (isSuggested) {
            Text(
                "推荐",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = accColor,
                modifier = Modifier
                    .background(accColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun templateIcon(template: ThinkingTemplate) = when (template) {
    ThinkingTemplate.cbt -> Icons.Default.Lightbulb
    ThinkingTemplate.socratic -> Icons.Default.QuestionAnswer
    ThinkingTemplate.behavioral -> Icons.Default.DirectionsRun
}

private fun templateLabel(template: ThinkingTemplate) = when (template) {
    ThinkingTemplate.cbt -> "CBT\n标准"
    ThinkingTemplate.socratic -> "苏格拉底\n提问"
    ThinkingTemplate.behavioral -> "行为\n激活"
}

// ═══════════════════════════════════════════════════
// MOOD PICKER
// ═══════════════════════════════════════════════════

@Composable
private fun MoodPicker(
    selectedMood: String,
    isAkathisia: Boolean,
    onMoodSelected: (String) -> Unit,
    onAkathisiaChanged: (Boolean) -> Unit,
    accColor: Color,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = accColor,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "点选当前心情",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "必选",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = accColor,
                    modifier = Modifier
                        .background(accColor.copy(alpha = 0.12f), RoundedCornerShape(50))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }

            Text(
                "不用选得很准，先点一个就能继续。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                sharedMoods.forEach { mood ->
                    val isSel = selectedMood == mood.label
                    FilterChip(
                        selected = isSel,
                        onClick = { onMoodSelected(mood.label) },
                        label = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(mood.emoji, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    mood.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accColor.copy(alpha = 0.18f),
                            selectedLabelColor = accColor,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSel) accColor.copy(alpha = 0.35f)
                                         else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            selectedBorderColor = accColor.copy(alpha = 0.35f),
                            enabled = true,
                            selected = isSel,
                        ),
                        shape = RoundedCornerShape(50),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Akathisia（静坐不能）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Switch(
                    checked = isAkathisia,
                    onCheckedChange = onAkathisiaChanged,
                    colors = SwitchDefaults.colors(checkedThumbColor = accColor),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// ANALYZE BUTTON
// ═══════════════════════════════════════════════════

@Composable
private fun AnalyzeBtn(
    flowStep: HomeFlowStep,
    isLoading: Boolean,
    isButtonPressed: Boolean,
    canSubmit: () -> Boolean,
    onClick: () -> Unit,
    onPressChanged: (Boolean) -> Unit,
) {
    val gs = MaterialTheme.colorScheme.primary
    val ge = MaterialTheme.colorScheme.tertiary
    val enabled = canSubmit()

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer(
                scaleX = if (isButtonPressed) 0.98f else 1f,
                scaleY = if (isButtonPressed) 0.98f else 1f,
            ),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
        ),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
        ),
        interactionSource = remember { MutableInteractionSource() },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        listOf(gs, ge),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, 0f),
                    ),
                    shape = RoundedCornerShape(18.dp),
                )
                .then(
                    if (enabled) {
                        Modifier.drawWithContent {
                            drawContent()
                            drawRoundRect(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color(0xFFFF6B6B),
                                        Color(0xFFFFD93D),
                                        Color(0xFF6BCB77),
                                        Color(0xFF4D96FF),
                                        Color(0xFFFF6B6B),
                                    ),
                                ),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx()),
                                alpha = 0.35f,
                            )
                        }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = if (flowStep == HomeFlowStep.ChooseMood)
                            Icons.Default.Refresh else Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buttonTitle(flowStep, isLoading),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
        }
    }
}

private fun buttonTitle(step: HomeFlowStep, loading: Boolean): String {
    if (loading) return "正在分析…"
    return when (step) {
        HomeFlowStep.WriteThought -> "下一步：选最省力的方式"
        HomeFlowStep.ChooseMode -> "下一步：点当前心情"
        HomeFlowStep.ChooseMood -> "开始分析"
    }
}

// ═══════════════════════════════════════════════════
// MORE OPTIONS SECTION
// ═══════════════════════════════════════════════════

@Composable
private fun MoreSection(
    expanded: Boolean,
    onToggle: (Boolean) -> Unit,
    accColor: Color,
    canCopy: () -> Boolean,
    onCopyPrompt: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle(!expanded) }
                .padding(horizontal = 2.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Default.MoreHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "更多选项",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        if (expanded) {
            val enabled = canCopy()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(1.dp, accColor.copy(alpha = 0.08f)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            Icons.Default.Savings,
                            contentDescription = null,
                            tint = accColor,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            "省钱 / 无 API",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "生成与站内一致的完整提示词并复制，再在外站免费或按次使用。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.95f),
                    )

                    Button(
                        onClick = onCopyPrompt,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accColor.copy(alpha = 0.08f),
                            contentColor = accColor,
                            disabledContainerColor = accColor.copy(alpha = 0.04f),
                            disabledContentColor = accColor.copy(alpha = 0.35f),
                        ),
                        border = BorderStroke(1.dp, accColor.copy(alpha = 0.22f)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "复制提示词并选择外站",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// QUICK START SECTION — VStack of Buttons, NOT LazyRow!
// ═══════════════════════════════════════════════════

@Composable
private fun QuickStart(
    prompts: List<Pair<String, String>>,
    onPromptSelected: (String) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFFFF9500),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "不知道怎么开始？试试这些：",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Vertical list of full-width Button rows — exactly like SwiftUI ForEach
            prompts.forEach { (emoji, text) ->
                Button(
                    onClick = { onPromptSelected(text) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(emoji, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// DEEP REASONING BANNER
// ═══════════════════════════════════════════════════

@Composable
private fun DeepReasonBanner(
    elapsedSeconds: Int,
    thinkingPhrase: String,
    accColor: Color,
) {
    val rotation by rememberInfiniteTransition(label = "spinner").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
        ),
        label = "rotation",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(40.dp)) {
                    drawCircle(
                        color = accColor.copy(alpha = 0.12f),
                        radius = 20.dp.toPx(),
                        style = Stroke(width = 2.5.dp.toPx()),
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(accColor, accColor.copy(alpha = 0.1f)),
                        ),
                        startAngle = rotation,
                        sweepAngle = 234f,
                        useCenter = false,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                Text(
                    "$elapsedSeconds",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    ),
                    color = accColor,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    thinkingPhrase,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "深度思考",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = accColor,
                        modifier = Modifier
                            .background(accColor.copy(alpha = 0.1f), RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                    Text(
                        "${elapsedSeconds}秒",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// GEMINI PRO BANNER
// ═══════════════════════════════════════════════════

@Composable
private fun GeminiBanner(
    onAppear: () -> Unit,
    accColor: Color,
) {
    LaunchedEffect(Unit) { onAppear() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, accColor.copy(alpha = 0.2f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = accColor,
                modifier = Modifier.size(18.dp),
            )
            Text(
                "Gemini Pro 正在组织回复",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 0..2) {
                    val dotScale by rememberInfiniteTransition(label = "dot_$i").animateFloat(
                        initialValue = 0.55f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500, easing = EaseInOutCubic),
                            repeatMode = RepeatMode.Reverse,
                            initialStartOffset = StartOffset(i * 120),
                        ),
                        label = "scale",
                    )
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(accColor)
                            .graphicsLayer(
                                scaleX = dotScale,
                                scaleY = dotScale,
                                alpha = dotScale,
                            ),
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

// ═══════════════════════════════════════════════════
// RETRY RECOVERY BANNER
// ═══════════════════════════════════════════════════

@Composable
private fun RetryBanner(message: String, accColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = accColor.copy(alpha = 0.1f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = accColor,
                modifier = Modifier.size(18.dp),
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ═══════════════════════════════════════════════════
// SAFETY BANNER VIEW
// ═══════════════════════════════════════════════════

@Composable
private fun SafetyBannerView() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "如果你正在经历危机，请立即寻求专业帮助。AI 不能替代专业的心理健康服务。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "\uD83D\uDCDE 心理援助热线：400-161-9995\n\uD83D\uDCDE 北京心理危机干预中心：010-82951332",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                lineHeight = 20.sp,
            )
        }
    }
}

// ═══════════════════════════════════════════════════
// ERROR BANNER
// ═══════════════════════════════════════════════════

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.08f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(18.dp),
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(20.dp),
            ) {
                Icon(
                    Icons.Default.Cancel,
                    contentDescription = "关闭",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// RECENT HISTORY PREVIEW
// ═══════════════════════════════════════════════════

@Composable
private fun RecentPreview(
    entries: List<HistoryEntity>,
    accColor: Color,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = accColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "最近的分析",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            entries.forEach { entry ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            entry.inputText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                entry.aiResponse?.take(20) ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                            Text(
                                relativeTime(entry.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun relativeTime(epochMillis: Long): String {
    val diff = System.currentTimeMillis() - epochMillis
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000} 分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
        else -> "${diff / 86_400_000} 天前"
    }
}

// ═══════════════════════════════════════════════════
// STREAMING RESULT VIEW
// ═══════════════════════════════════════════════════

@Composable
private fun StreamingView(text: String) {
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "分段生成中",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (text.isEmpty()) {
                Text(
                    "正在生成…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                chunks.take(if (showAll) chunks.size else visibleCount.coerceIn(1, chunks.size))
                    .forEach { chunk ->
                        val highlighted = chunk.contains("替代想法") ||
                            chunk.contains("下一步行动") ||
                            chunk.contains("积极视角")
                        Text(
                            text = chunk,
                            modifier = if (highlighted)
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        RoundedCornerShape(10.dp),
                                    )
                                    .padding(10.dp)
                            else Modifier.padding(vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (highlighted) FontWeight.Medium else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { isPaused = !isPaused },
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(if (isPaused) "继续" else "暂停", fontSize = 13.sp)
                }
                Button(
                    onClick = { showAll = !showAll },
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(if (showAll) "分段查看" else "显示全部", fontSize = 13.sp)
                }
            }
        }
    }
}
