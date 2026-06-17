package com.henryliu.cbtreframe.android.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.henryliu.cbtreframe.shared.*
import com.henryliu.cbtreframe.shared.db.HistoryEntity
import com.henryliu.cbtreframe.ui.WindowSizeState
import com.henryliu.cbtreframe.ui.WindowWidthSizeClass

@Composable
fun HomeView(
    viewModel: ReframeViewModel,
    globalSettings: GlobalSettings,
    recentHistory: List<HistoryEntity> = emptyList(),
    onTemplateChanged: (ThinkingTemplate) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val focus = LocalFocusManager.current
    val acc = MaterialTheme.colorScheme.primary

    var inputFocused by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var showExternal by remember { mutableStateOf(false) }
    var geminiPulse by remember { mutableStateOf(false) }
    var btnPressed by remember { mutableStateOf(false) }

    val text = uiState.inputText
    val result = uiState.result
    val loading = uiState.isLoading
    val streaming = uiState.isStreamingResult
    val homeStage = uiState.homeStage

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshProviderAndModel()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeBackground(acc)

        val windowSizeClass = WindowSizeState.current
        val horizontalPadding = if (windowSizeClass == WindowWidthSizeClass.Expanded) 40.dp else 20.dp

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
                modifier = Modifier.padding(horizontal = horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                HeaderSection(greeting = uiState.greeting, quote = uiState.todayQuote)
                TodayDashboard(uiState.currentStreak, uiState.todayAnalysisCount, uiState.longestStreak, acc)

                if (result != null) {
                    NewAnalysisBtn(onClick = { viewModel.reset() })
                }

                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    ThoughtInputCard(
                        inputText = text,
                        onInputChanged = { viewModel.setInputText(it) },
                        isFocused = inputFocused,
                        onFocusChanged = { inputFocused = it },
                        accColor = acc
                    )

                    AnimatedVisibility(
                        visible = homeStage.order >= HomeStage.ChoosingMode.order,
                        enter = fadeIn() + slideInVertically { -it / 4 },
                        exit = fadeOut() + slideOutVertically { -it / 4 }
                    ) {
                        TemplatePicker(
                            selectedTemplate = globalSettings.thinkingTemplate,
                            suggestedTemplate = uiState.suggestedThinkingTemplate,
                            onTemplateSelected = {
                                onTemplateChanged(it)
                                viewModel.setSelectedTemplate(it)
                            },
                            accColor = acc
                        )
                    }

                    AnimatedVisibility(
                        visible = homeStage.order >= HomeStage.ChoosingMood.order,
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
                        homeStage = homeStage,
                        isLoading = loading,
                        isButtonPressed = btnPressed,
                        canSubmit = {
                            val t = text.trim()
                            when (homeStage) {
                                HomeStage.QuickStart, HomeStage.WritingThought -> t.isNotEmpty()
                                HomeStage.ChoosingMode -> t.isNotEmpty()
                                else -> t.isNotEmpty() && uiState.selectedMood.trim().isNotEmpty()
                            }
                        },
                        onClick = {
                            focus.clearFocus()
                            when (homeStage) {
                                HomeStage.QuickStart, HomeStage.WritingThought, HomeStage.ChoosingMode -> {
                                    // If no template explicitly chosen yet, proceed with current global default
                                    viewModel.setSelectedTemplate(globalSettings.thinkingTemplate)
                                }
                                else -> viewModel.analyzeThought(globalSettings)
                            }
                        },
                        onPressChanged = { btnPressed = it }
                    )
                }

                AnimatedVisibility(
                    visible = homeStage.order >= HomeStage.ChoosingMode.order,
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

                AnimatedVisibility(
                    visible = homeStage == HomeStage.QuickStart,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                ) {
                    QuickStart(
                        prompts = ReframeViewModel.quickStartPrompts,
                        onPromptSelected = { viewModel.setInputText(it) }
                    )
                }

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
                            historyEntryID = uiState.latestHistoryEntryID,
                            providerRaw = uiState.selectedProvider.name,
                            modelRaw = uiState.selectedModelName
                        )
                    }
                }

                if (streaming) {
                    StreamingView(text = uiState.streamingText)
                }

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
            title = { Text("已复制到剪贴板") },
            text = { Text("在新建对话里粘贴刚才复制的全部内容即可。") },
            confirmButton = {
                TextButton(onClick = { showExternal = false }) { Text("完成") }
            }
        )
    }
}

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
            SectionHeader(
                icon = Icons.Default.MoreHoriz,
                title = "更多选项",
                accColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                            modifier = Modifier.size(16.dp),
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
            SectionHeader(
                icon = Icons.Default.Lightbulb,
                title = "不知道怎么开始？试试这些：",
                accColor = Color(0xFFFF9500),
            )

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
                        Text(emoji, fontSize = 20.sp)
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

@Composable
private fun StreamingView(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "正在生成",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (text.isEmpty()) {
                Text(
                    "思考中…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    accColor: Color,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accColor,
            modifier = Modifier.size(20.dp),
        )
        Text(
            title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.4.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (trailingContent != null) {
            trailingContent()
        }
    }
}
