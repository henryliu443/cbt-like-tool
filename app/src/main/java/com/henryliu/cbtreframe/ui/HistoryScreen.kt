package com.henryliu.cbtreframe.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.henryliu.cbtreframe.shared.*
import com.henryliu.cbtreframe.shared.db.HistoryEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel,
    globalSettings: GlobalSettings = GlobalSettings.Default,
) {
    val uiState by viewModel.uiState.collectAsState()
    val entries by viewModel.history.collectAsState()
    val settingsUiState by settingsViewModel.uiState.collectAsState()

    // Face ID lock state
    var isUnlocked by remember { mutableStateOf(false) }
    var hasAttemptedAuth by remember { mutableStateOf(false) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }

    val needsAuth = settingsUiState.useFaceID && !isUnlocked

    LaunchedEffect(settingsUiState.useFaceID) {
        if (settingsUiState.useFaceID && !hasAttemptedAuth) {
            hasAttemptedAuth = true
            val ok = settingsViewModel.authenticateWithBiometrics("解锁历史记录")
            if (ok) {
                isUnlocked = true
                authErrorMessage = null
            } else {
                authErrorMessage = "验证失败，请重试"
            }
        } else if (!settingsUiState.useFaceID) {
            isUnlocked = true
            authErrorMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史记录") },
                actions = {
                    if (!needsAuth && entries.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.setShowFavoritesOnly(!uiState.showFavoritesOnly)
                        }) {
                            Icon(
                                if (uiState.showFavoritesOnly) Icons.Default.Star else Icons.Default.Star,
                                contentDescription = if (uiState.showFavoritesOnly) "显示全部" else "仅显示收藏",
                                tint = if (uiState.showFavoritesOnly)
                                    MaterialTheme.colorScheme.tertiary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (needsAuth) {
            // ── Locked State ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    "历史记录已锁定",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "点击下方按钮使用生物识别解锁",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                authErrorMessage?.let { error ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        val scope = kotlinx.coroutines.MainScope()
                        scope.launch {
                            hasAttemptedAuth = true
                            val ok = settingsViewModel.authenticateWithBiometrics("解锁历史记录")
                            if (ok) {
                                isUnlocked = true
                                authErrorMessage = null
                            } else {
                                authErrorMessage = "验证失败，请重试"
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Face, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("生物识别解锁", fontWeight = FontWeight.Bold)
                }
            }
        } else if (entries.isEmpty()) {
            // ── Empty State ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "还没有历史记录",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "完成一次分析后，记录将出现在这里",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // ── List Content ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Search bar
                OutlinedTextField(
                    value = uiState.searchText,
                    onValueChange = { viewModel.setSearchText(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("搜索想法、心情、扭曲、行动…") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.searchText.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchText("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                // Weekly stats
                if (uiState.weeklyCount > 0) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text("本周 ${uiState.weeklyCount} 条") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                        if (uiState.weeklyFavoriteCount > 0) {
                            AssistChip(
                                onClick = {},
                                label = { Text("收藏 ${uiState.weeklyFavoriteCount} 条") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // Grouped list
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    uiState.groupedEntries.forEach { group ->
                        item(key = "header_${group.dateLabel}") {
                            Text(
                                group.dateLabel,
                                modifier = Modifier.padding(vertical = 12.dp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(
                            items = group.entries,
                            key = { it.id }
                        ) { entry ->
                            HistoryEntryCard(
                                entry = entry,
                                viewModel = viewModel,
                                globalSettings = globalSettings
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── History Entry Card ──

@Composable
private fun HistoryEntryCard(
    entry: HistoryEntity,
    viewModel: HistoryViewModel,
    globalSettings: GlobalSettings,
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val template = try {
        ThinkingTemplate.valueOf(entry.therapyTemplateRaw)
    } catch (_: Exception) {
        ThinkingTemplate.cbt
    }

    val analysisResult = try {
        kotlinx.serialization.json.Json.decodeFromString<AnalysisResult>(
            entry.resultExtrasJSON
        )
    } catch (_: Exception) {
        AnalysisResult(
            distortion = entry.distortion,
            alternative = entry.alternative,
            action = entry.action
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    entry.inputThought,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(12.dp))
                IconButton(
                    onClick = { viewModel.toggleFavorite(entry) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (entry.isFavorite != 0L) Icons.Default.Star else Icons.Default.Star,
                        contentDescription = "收藏",
                        tint = if (entry.isFavorite != 0L)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Tags row
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Template tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Text(
                        template.displayName(),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (entry.moodTag.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            entry.moodTag,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                ) {
                    Text(
                        entry.distortion.ifBlank { "未识别" },
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(Modifier.weight(1f))

                Text(
                    HistoryViewModel.formatTimestamp(entry.createdAt),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expanded content — ResultCard
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    ResultCardScreen(
                        result = analysisResult,
                        template = template,
                        inputThought = entry.inputThought,
                        moodTag = entry.moodTag,
                        historyEntryID = entry.id
                    )
                    Spacer(Modifier.height(8.dp))
                    // Delete button
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("删除此记录", fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("此操作不可撤销。确定要删除这条记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteItem(entry.id)
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}
