package com.henryliu.cbtreframe.android.ui

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.henryliu.cbtreframe.shared.AnalysisResult
import com.henryliu.cbtreframe.shared.HistoryResultExtras
import com.henryliu.cbtreframe.shared.SettingsViewModel
import com.henryliu.cbtreframe.shared.ThinkingTemplate
import com.henryliu.cbtreframe.shared.db.HistoryEntity
import com.henryliu.cbtreframe.shared.viewmodels.HistoryViewModel
import com.henryliu.cbtreframe.ui.displayName
import org.koin.compose.koinInject

private val ThinkingTemplate.historyTag: String
    get() = when (this) {
        ThinkingTemplate.cbt -> "CBT"
        ThinkingTemplate.socratic -> "苏格拉底"
        ThinkingTemplate.behavioral -> "行为"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryView(
    viewModel: HistoryViewModel = koinInject(),
    settingsViewModel: SettingsViewModel = koinInject()
) {
    DisposableEffect(Unit) { onDispose { viewModel.clear() } }

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val allEntries by viewModel.history.collectAsState()

    val settingsState by settingsViewModel.uiState.collectAsState()
    val useFaceID = settingsState.useFaceID

    val biometricManager = remember(context) { BiometricManager.from(context) }
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    val canAuthenticate = remember(biometricManager) {
        biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    var isUnlocked by remember { mutableStateOf(!useFaceID || !canAuthenticate) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var hasAttemptedAuth by remember { mutableStateOf(false) }

    val needsAuth = useFaceID && !isUnlocked && canAuthenticate

    val authenticateIfNeeded = {
        if (canAuthenticate && !isUnlocked) {
            val executor = ContextCompat.getMainExecutor(context)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("解锁历史记录")
                .setSubtitle("请进行身份验证以查看历史记录")
                .setAllowedAuthenticators(authenticators)
                .build()

            val activity = context as? FragmentActivity
            if (activity != null) {
                val biometricPrompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            authErrorMessage = "验证出错: $errString"
                        }

                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            isUnlocked = true
                            authErrorMessage = null
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            authErrorMessage = "验证失败，请重试"
                        }
                    }
                )
                biometricPrompt.authenticate(promptInfo)
            } else {
                authErrorMessage = "需要 Activity 上下文进行身份验证"
            }
        }
    }

    // Auto trigger auth on entry
    LaunchedEffect(useFaceID) {
        if (useFaceID && !isUnlocked && !hasAttemptedAuth) {
            hasAttemptedAuth = true
            authenticateIfNeeded()
        } else if (!useFaceID) {
            isUnlocked = true
        }
    }

    // App background auto lock
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, useFaceID) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (useFaceID && canAuthenticate) {
                    isUnlocked = false
                    hasAttemptedAuth = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (needsAuth) {
        LockedState(
            authErrorMessage = authErrorMessage,
            onRetry = { authenticateIfNeeded() },
            onDisableLock = {
                settingsViewModel.setUseFaceID(false)
                isUnlocked = true
                authErrorMessage = null
            }
        )
        return
    }

    var showExportOptions by remember { mutableStateOf(false) }

    val exportAction = { format: String ->
        val file = when (format) {
            "json" -> HistoryExportManager.makeTemporaryJSONFile(context, allEntries)
            "csv" -> HistoryExportManager.makeTemporaryCSVFile(context, allEntries)
            "pdf" -> HistoryExportManager.makeTemporaryPDFFile(context, allEntries)
            else -> null
        }
        if (file != null) {
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = when (format) {
                    "json" -> "application/json"
                    "csv" -> "text/csv"
                    "pdf" -> "application/pdf"
                    else -> "*/*"
                }
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "导出历史记录"))
        }
        showExportOptions = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史记录", fontWeight = FontWeight.Bold) },
                actions = {
                    if (!allEntries.isEmpty()) {
                        Box {
                            IconButton(onClick = { showExportOptions = true }) {
                                Icon(Icons.Default.Share, contentDescription = "导出")
                            }
                            DropdownMenu(
                                expanded = showExportOptions,
                                onDismissRequest = { showExportOptions = false }
                            ) {
                                DropdownMenuItem(text = { Text("导出为 JSON") }, onClick = { exportAction("json") })
                                DropdownMenuItem(text = { Text("导出为 CSV") }, onClick = { exportAction("csv") })
                                DropdownMenuItem(text = { Text("导出为 PDF") }, onClick = { exportAction("pdf") })
                            }
                        }
                        IconButton(
                            onClick = { viewModel.setShowFavoritesOnly(!uiState.showFavoritesOnly) }
                        ) {
                            Icon(
                                imageVector = if (uiState.showFavoritesOnly) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (uiState.showFavoritesOnly) Color.Yellow else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = uiState.searchText,
                onValueChange = { viewModel.setSearchText(it) },
                placeholder = { Text("搜索想法、心情、扭曲、行动…", fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            if (allEntries.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "weekly_review") {
                        val stats = viewModel.weeklyStats(allEntries)
                        val topMoodEmoji = remember(allEntries) {
                            // 限制统计成本，仅取前 50 条统计最常见心情 Emoji（与 iOS 行为完全一致）
                            val moods = allEntries.take(50).map { it.moodTag }.filter { it.isNotBlank() }
                            if (moods.isEmpty()) "–"
                            else {
                                val counts = moods.groupBy { it }.mapValues { it.value.size }
                                val topMood = counts.maxByOrNull { it.value }?.key ?: "–"
                                getMoodEmoji(topMood)
                            }
                        }
                        WeeklyReviewCard(
                            stats = stats,
                            totalCount = allEntries.size,
                            topMoodEmoji = topMoodEmoji
                        )
                    }

                    uiState.groupedEntries.forEach { group ->
                        item(key = "header_${group.dateLabel}") {
                            Text(
                                text = group.dateLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(
                            items = group.entries,
                            key = { entry -> entry.id }
                        ) { entry ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissValue ->
                                    when (dismissValue) {
                                        SwipeToDismissBoxValue.EndToStart -> {
                                            viewModel.deleteItem(entry.id)
                                            true
                                        }
                                        SwipeToDismissBoxValue.StartToEnd -> {
                                            val result = AnalysisResult(
                                                distortion = entry.distortion,
                                                alternative = entry.alternative,
                                                action = entry.action
                                            )
                                            val displayTemplate = try { ThinkingTemplate.valueOf(entry.therapyTemplateRaw) } catch (e: Exception) { ThinkingTemplate.cbt }
                                            val depthLabel = try { ThinkingTemplate.AnalysisDepth.valueOf(entry.analysisDepthRaw).displayName() } catch (e: Exception) { "" }
                                            val clipboardText = buildClipboardText(result, displayTemplate, entry.inputText, entry.moodTag, depthLabel)
                                            
                                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(android.content.Intent.EXTRA_TEXT, clipboardText)
                                            }
                                            context.startActivity(android.content.Intent.createChooser(shareIntent, "分享分析结果"))
                                            false
                                        }
                                        else -> false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val color = when (dismissState.targetValue) {
                                        SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = 0.8f)
                                        SwipeToDismissBoxValue.StartToEnd -> Color.Blue.copy(alpha = 0.8f)
                                        else -> Color.Transparent
                                    }

                                    val alignment = when (dismissState.targetValue) {
                                        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                        else -> Alignment.Center
                                    }

                                    val icon = when (dismissState.targetValue) {
                                        SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                                        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Share
                                        else -> null
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(color)
                                            .padding(horizontal = 24.dp),
                                        contentAlignment = alignment
                                    ) {
                                        if (icon != null) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                },
                                content = {
                                    HistoryEntryCard(
                                        entry = entry,
                                        viewModel = viewModel
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryEntryCard(
    entry: HistoryEntity,
    viewModel: HistoryViewModel,
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }

    val displayTemplate = remember(entry.therapyTemplateRaw) {
        try {
            ThinkingTemplate.valueOf(entry.therapyTemplateRaw)
        } catch (e: Exception) {
            ThinkingTemplate.cbt
        }
    }

    val depthLabel = remember(entry.analysisDepthRaw) {
        try {
            ThinkingTemplate.AnalysisDepth.valueOf(entry.analysisDepthRaw).displayName()
        } catch (e: Exception) {
            ""
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .combinedClickable(
                onClick = { isExpanded = !isExpanded },
                onLongClick = { showContextMenu = true }
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {

                Text(
                    text = entry.inputText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(12.dp))

                IconButton(
                    onClick = { viewModel.toggleFavorite(entry) },
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Top)
                ) {
                    Icon(
                        imageVector = if (entry.isFavorite == 1L) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = if (entry.isFavorite == 1L) Color.Yellow else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    // Capsule metadata layout
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CapsuleLabel(
                            text = displayTemplate.historyTag,
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )

                        if (entry.moodTag.isNotBlank()) {
                            CapsuleLabel(
                                text = entry.moodTag,
                                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (depthLabel.isNotBlank()) {
                            CapsuleLabel(
                                text = depthLabel,
                                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (entry.distortion.isNotBlank()) {
                            CapsuleLabel(
                                text = entry.distortion,
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                contentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }

                        if (entry.providerName.isNotBlank()) {
                            Text(
                                text = entry.providerName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = formatTimeToOnlyTime(entry.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 1.dp)
                    Spacer(Modifier.height(12.dp))

                    val extras = remember(entry.resultExtrasJSON) {
                        try {
                            kotlinx.serialization.json.Json.decodeFromString<HistoryResultExtras>(entry.resultExtrasJSON)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    val result = AnalysisResult(
                        distortion = entry.distortion,
                        alternative = entry.alternative,
                        action = entry.action,
                        questions = extras?.questions,
                        actions = extras?.actions,
                        stateAssessment = extras?.stateAssessment
                    )

                    ResultCardView(
                        result = result,
                        template = displayTemplate,
                        inputThought = entry.inputText,
                        moodTag = entry.moodTag,
                        analysisDepthLabel = depthLabel,
                        historyEntryID = entry.id,
                        followUpMessagesJSON = entry.followUpMessagesJSON
                    )
                }
            }
        }

        Box {
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("分享") },
                    onClick = {
                        showContextMenu = false
                        val clipboardText = buildClipboardText(
                            result = AnalysisResult(
                                distortion = entry.distortion,
                                alternative = entry.alternative,
                                action = entry.action
                            ),
                            template = displayTemplate,
                            inputThought = entry.inputText,
                            moodTag = entry.moodTag,
                            analysisDepthLabel = depthLabel
                        )
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, clipboardText)
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "分享分析结果"))
                    }
                )
                DropdownMenuItem(
                    text = { Text("移除", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showContextMenu = false
                        viewModel.deleteItem(entry.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun CapsuleLabel(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LockedState(
    authErrorMessage: String?,
    onRetry: () -> Unit,
    onDisableLock: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )

            Text(
                text = "历史记录已锁定",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "使用身份验证解锁",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!authErrorMessage.isNullOrBlank()) {
                Text(
                    text = authErrorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("身份验证解锁", fontWeight = FontWeight.Bold)
                }
            }

            TextButton(onClick = onDisableLock) {
                Text(
                    text = "禁用锁定并进入",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "还没有记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "完成你的第一次思维重构后\n记录会出现在这里",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun WeeklyReviewCard(
    stats: Pair<Int, Int>,
    totalCount: Int,
    topMoodEmoji: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "本周回顾",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(
                    value = "${stats.first}",
                    label = "本周分析",
                    icon = Icons.Default.Build,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatDivider()
                StatItem(
                    value = "${stats.second}",
                    label = "收藏",
                    icon = Icons.Default.Star,
                    color = Color.Yellow,
                    modifier = Modifier.weight(1f)
                )
                StatDivider()
                StatItem(
                    value = "$totalCount",
                    label = "总记录",
                    icon = Icons.Default.DateRange,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                StatDivider()
                StatItem(
                    value = topMoodEmoji,
                    label = "常见心情",
                    icon = null,
                    color = Color.Unspecified,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: ImageVector?,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    )
}

private fun getMoodEmoji(moodLabel: String): String {
    return when (moodLabel) {
        "低落" -> "😔"
        "焦虑" -> "😰"
        "愤怒" -> "😤"
        "担忧" -> "😟"
        "失望" -> "😞"
        "疲惫" -> "🫠"
        "麻木" -> "😶"
        "内在不安" -> "😣"
        "开心" -> "🥳"
        "愉快" -> "😆"
        else -> "🙂"
    }
}

private fun formatTimeToOnlyTime(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(date)
}


private fun buildClipboardText(
    result: AnalysisResult,
    template: ThinkingTemplate,
    inputThought: String,
    moodTag: String,
    analysisDepthLabel: String
): String {
    var text = ""
    val mood = moodTag.trim()
    if (mood.isNotEmpty()) {
        text += "心情：$mood\n"
    }
    val depth = analysisDepthLabel.trim()
    if (depth.isNotEmpty()) {
        text += "深度：$depth\n"
    }
    if (inputThought.isNotEmpty()) {
        text += "我的想法：$inputThought\n\n"
    }
    when (template) {
        ThinkingTemplate.cbt -> {
            text += """
            认知扭曲：${result.distortion}
            替代想法：${result.alternative}
            建议行动：${result.action}
            """.trimIndent()
        }
        ThinkingTemplate.socratic -> {
            val qs = (result.questions ?: emptyList()).mapIndexed { idx, q -> "${idx + 1}. $q" }.joinToString("\n")
            text += """
            引导问题：
            $qs

            说明：${result.alternative}
            反思练习：${result.action}
            """.trimIndent()
        }
        ThinkingTemplate.behavioral -> {
            text += """
            状态：${result.stateAssessment ?: result.distortion}
            下一步：${result.action}
            积极视角：${result.alternative}
            """.trimIndent()
        }
    }
    text += "\n\n请帮我进一步分析这个想法，给出更多角度和建议。"
    return text
}
