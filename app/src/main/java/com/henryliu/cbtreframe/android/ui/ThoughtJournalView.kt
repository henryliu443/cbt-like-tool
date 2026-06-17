package com.henryliu.cbtreframe.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.henryliu.cbtreframe.shared.ReframeOrchestrator
import com.henryliu.cbtreframe.shared.ThoughtEntry
import com.henryliu.cbtreframe.shared.ThoughtJournalViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThoughtJournalView(viewModel: ThoughtJournalViewModel = koinInject()) {
    DisposableEffect(Unit) { onDispose { viewModel.clear() } }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    if (uiState.showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowAddSheet(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ) {
            AddThoughtSheetContent(
                viewModel = viewModel,
                onSave = {
                    viewModel.saveQuickCaptureEntry()
                    viewModel.setShowAddSheet(false)
                },
                onCancel = { viewModel.setShowAddSheet(false) }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("想法记录") },
                actions = {
                    val unprocessed = uiState.entries.filter { !it.isProcessed }
                    if (unprocessed.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.analyzePatterns()
                            },
                            enabled = !uiState.isAnalyzing
                        ) {
                            if (uiState.isAnalyzing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "整理")
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setShowAddSheet(true) },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加想法")
            }
        }
    ) { innerPadding ->
        if (uiState.entries.isEmpty() && uiState.patternReport == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "记录你的自动想法",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "当脑海中闪过负面想法时\n随手记下来，积累后让 AI 帮你整理",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.errorMessage?.let { error ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                uiState.patternReport?.let { report ->
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("思维模式分析", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                report.topDistortions.forEach { distortion ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${distortion.count}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(distortion.name, fontWeight = FontWeight.SemiBold)
                                            Text(distortion.example, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                Text("整体模式", fontWeight = FontWeight.SemiBold)
                                Text(report.overallPattern, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("建议", fontWeight = FontWeight.SemiBold, color = Color(0xFFFFA500)) // Orange
                                Text(report.suggestion, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                val unprocessed = uiState.entries.filter { !it.isProcessed }
                if (unprocessed.isNotEmpty()) {
                    item {
                        Text("待整理 (${unprocessed.size}条)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                    }
                    items(unprocessed, key = { it.id }) { entry ->
                        ThoughtEntryRow(entry, onDelete = { viewModel.deleteThought(it.id) })
                    }
                }

                val processed = uiState.entries.filter { it.isProcessed }
                if (processed.isNotEmpty()) {
                    item {
                        Text("已整理", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                    }
                    items(processed, key = { it.id }) { entry ->
                        ThoughtEntryRow(entry, onDelete = { viewModel.deleteThought(it.id) })
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun ThoughtEntryRow(entry: ThoughtEntry, onDelete: (ThoughtEntry) -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(entry.content, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (entry.emotion.isNotBlank()) {
                    SuggestionChip(
                        onClick = { },
                        label = { Text(entry.emotion) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    )
                }
                if (entry.distortionTag.isNotBlank()) {
                    SuggestionChip(
                        onClick = { },
                        label = { Text(entry.distortionTag) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFFFA500).copy(alpha = 0.2f))
                    )
                }
            }
            if (entry.balancedThought.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("平衡想法：${entry.balancedThought}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { onDelete(entry) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddThoughtSheetContent(
    viewModel: ThoughtJournalViewModel,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val emotions = listOf("😔 低落", "😰 焦虑", "😤 愤怒", "😟 担忧", "😞 失望", "🫠 疲惫", "😶 麻木", "😨 恐惧")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) { Text("取消") }
            Text("记录想法", style = MaterialTheme.typography.titleMedium)
            TextButton(
                onClick = onSave,
                enabled = uiState.quickInput.isNotBlank()
            ) { Text("保存", fontWeight = FontWeight.Bold) }
        }

        OutlinedTextField(
            value = uiState.quickInput,
            onValueChange = { viewModel.setQuickInput(it) },
            label = { Text("脑海中闪过的想法") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        OutlinedTextField(
            value = uiState.situation,
            onValueChange = { viewModel.setSituation(it) },
            label = { Text("当时的情境（可选）") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("情绪", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            emotions.forEach { emotion ->
                val isSelected = uiState.selectedEmotion == emotion
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setSelectedEmotion(if (isSelected) "" else emotion) },
                    label = { Text(emotion) }
                )
            }
        }

        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("强度", style = MaterialTheme.typography.titleSmall)
                Text("${uiState.intensity.toInt()}/10", color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = uiState.intensity.toFloat(),
                onValueChange = { viewModel.setIntensity(it.toDouble()) },
                valueRange = 1f..10f,
                steps = 8
            )
        }

        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("信念强度（分析前）", style = MaterialTheme.typography.titleSmall)
                Text("${uiState.beliefBefore.toInt()}%", color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = uiState.beliefBefore.toFloat(),
                onValueChange = { viewModel.setBeliefBefore(it.toDouble()) },
                valueRange = 0f..100f,
                steps = 99
            )
        }

        OutlinedTextField(
            value = uiState.evidenceFor,
            onValueChange = { viewModel.setEvidenceFor(it) },
            label = { Text("支持这个想法的证据（可选）") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.evidenceAgainst,
            onValueChange = { viewModel.setEvidenceAgainst(it) },
            label = { Text("反对这个想法的证据（可选）") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
