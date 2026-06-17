package com.henryliu.cbtreframe.android.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.henryliu.cbtreframe.shared.AnalysisResult
import com.henryliu.cbtreframe.shared.ThinkingTemplate
import kotlinx.coroutines.delay

@Composable
fun ResultCardView(
    result: AnalysisResult,
    template: ThinkingTemplate = ThinkingTemplate.cbt,
    inputThought: String = "",
    moodTag: String = "",
    analysisDepthLabel: String = "",
    historyEntryID: String? = null,
    followUpMessagesJSON: String = "",
    providerRaw: String = "",
    modelRaw: String = "",
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    var copiedToast by remember { mutableStateOf(false) }
    var collapsedSections by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(copiedToast) {
        if (copiedToast) {
            delay(1500)
            copiedToast = false
        }
    }

    val allLabels = remember(result, template) {
        when (template) {
            ThinkingTemplate.cbt -> {
                val list = mutableListOf("认知扭曲", "替代想法", "建议行动")
                if (!result.actions.isNullOrEmpty()) list.add("更多行动")
                list.toSet()
            }
            ThinkingTemplate.socratic -> {
                val list = mutableListOf<String>()
                if (result.distortion.isNotEmpty()) list.add("视角提示")
                list.add("引导问题")
                if (result.alternative.isNotEmpty()) list.add("说明")
                list.add("反思练习")
                list.toSet()
            }
            ThinkingTemplate.behavioral -> {
                val list = mutableListOf("当前状态", "下一步行动")
                if (result.alternative.isNotEmpty()) list.add("积极视角")
                list.toSet()
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Key Insight Header
                val keyInsightText = remember(result, template) { getKeyInsightText(result, template) }
                if (keyInsightText != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "关键提醒",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = keyInsightText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DividerLine()
                }

                // Template specific rendering
                when (template) {
                    ThinkingTemplate.cbt -> {
                        ResultSection(
                            icon = Icons.Default.Warning,
                            iconColor = Color(0xFFE65100),
                            label = "认知扭曲",
                            value = result.distortion,
                            isCollapsed = collapsedSections.contains("认知扭曲"),
                            onToggle = { toggleSection("认知扭曲", collapsedSections) { collapsedSections = it } }
                        )
                        DividerLine()
                        ResultSection(
                            icon = Icons.Default.Star,
                            iconColor = MaterialTheme.colorScheme.primary,
                            label = "替代想法",
                            value = result.alternative,
                            isCollapsed = collapsedSections.contains("替代想法"),
                            onToggle = { toggleSection("替代想法", collapsedSections) { collapsedSections = it } }
                        )
                        DividerLine()
                        ResultSection(
                            icon = Icons.Default.PlayArrow,
                            iconColor = Color(0xFF2E7D32),
                            label = "建议行动",
                            value = result.action,
                            isCollapsed = collapsedSections.contains("建议行动"),
                            onToggle = { toggleSection("建议行动", collapsedSections) { collapsedSections = it } }
                        )
                        val actions = result.actions
                        if (!actions.isNullOrEmpty()) {
                            DividerLine()
                            ResultSection(
                                icon = Icons.Default.List,
                                iconColor = Color(0xFF008080),
                                label = "更多行动",
                                value = actions.joinToString("\n"),
                                isCollapsed = collapsedSections.contains("更多行动"),
                                onToggle = { toggleSection("更多行动", collapsedSections) { collapsedSections = it } }
                            )
                        }

                    }
                    ThinkingTemplate.socratic -> {
                        if (result.distortion.isNotEmpty()) {
                            ResultSection(
                                icon = Icons.Default.Info,
                                iconColor = Color(0xFFE65100),
                                label = "视角提示",
                                value = result.distortion,
                                isCollapsed = collapsedSections.contains("视角提示"),
                                onToggle = { toggleSection("视角提示", collapsedSections) { collapsedSections = it } }
                            )
                            DividerLine()
                        }
                        SocraticQuestionsSection(
                            result = result,
                            isCollapsed = collapsedSections.contains("引导问题"),
                            onToggle = { toggleSection("引导问题", collapsedSections) { collapsedSections = it } }
                        )
                        if (result.alternative.isNotEmpty()) {
                            DividerLine()
                            ResultSection(
                                icon = Icons.Default.Star,
                                iconColor = MaterialTheme.colorScheme.primary,
                                label = "说明",
                                value = result.alternative,
                                isCollapsed = collapsedSections.contains("说明"),
                                onToggle = { toggleSection("说明", collapsedSections) { collapsedSections = it } }
                            )
                        }
                        DividerLine()
                        ResultSection(
                            icon = Icons.Default.Edit,
                            iconColor = Color(0xFF2E7D32),
                            label = "反思练习",
                            value = result.action,
                            isCollapsed = collapsedSections.contains("反思练习"),
                            onToggle = { toggleSection("反思练习", collapsedSections) { collapsedSections = it } }
                        )
                    }
                    ThinkingTemplate.behavioral -> {
                        ResultSection(
                            icon = Icons.Default.Favorite,
                            iconColor = Color(0xFFE65100),
                            label = "当前状态",
                            value = result.stateAssessment ?: result.distortion,
                            isCollapsed = collapsedSections.contains("当前状态"),
                            onToggle = { toggleSection("当前状态", collapsedSections) { collapsedSections = it } }
                        )
                        DividerLine()
                        ResultSection(
                            icon = Icons.Default.ArrowForward,
                            iconColor = MaterialTheme.colorScheme.primary,
                            label = "下一步行动",
                            value = result.action,
                            isCollapsed = collapsedSections.contains("下一步行动"),
                            onToggle = { toggleSection("下一步行动", collapsedSections) { collapsedSections = it } }
                        )
                        if (result.alternative.isNotEmpty()) {
                            DividerLine()
                            ResultSection(
                                icon = Icons.Default.PlayArrow,
                                iconColor = Color(0xFF2E7D32),
                                label = "积极视角",
                                value = result.alternative,
                                isCollapsed = collapsedSections.contains("积极视角"),
                                onToggle = { toggleSection("积极视角", collapsedSections) { collapsedSections = it } }
                            )
                        }
                    }
                }

                DividerLine()

                // Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = LocalContext.current

                    // Button 1: 复制
                    TextButton(
                        onClick = {
                            val textToCopy = buildClipboardText(result, template, inputThought, moodTag, analysisDepthLabel)
                            clipboardManager.setText(AnnotatedString(textToCopy))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            copiedToast = true
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("复制", fontSize = 13.sp)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    )

                    // Button 2: 发到 ChatGPT
                    TextButton(
                        onClick = {
                            val textToCopy = buildClipboardText(result, template, inputThought, moodTag, analysisDepthLabel)
                            clipboardManager.setText(AnnotatedString(textToCopy))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            copiedToast = true
                            
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                data = android.net.Uri.parse("https://chatgpt.com")
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Ignored
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("发到 ChatGPT", fontSize = 13.sp)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    )

                    // Button 3: 展开/收起分段
                    TextButton(
                        onClick = {
                            if (collapsedSections.isEmpty()) {
                                collapsedSections = allLabels
                            } else {
                                collapsedSections = emptySet()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (collapsedSections.isEmpty()) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (collapsedSections.isEmpty()) "收起分段" else "展开分段",
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                
                var showFollowUp by remember { mutableStateOf(false) }

                // Continue Exploration Footer INSIDE the Card
                Button(
                    onClick = { showFollowUp = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .padding(bottom = 4.dp), // Extra padding at bottom of card
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowCircleRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "继续探索",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                if (showFollowUp) {
                    androidx.compose.ui.window.Dialog(
                        onDismissRequest = { showFollowUp = false },
                        properties = androidx.compose.ui.window.DialogProperties(
                            usePlatformDefaultWidth = false,
                            decorFitsSystemWindows = false
                        )
                    ) {
                        val vm = org.koin.compose.koinInject<com.henryliu.cbtreframe.shared.viewmodels.HistoryViewModel>()
                        FollowUpChatView(
                            entryId = historyEntryID ?: "",
                            originalThought = inputThought,
                            lastConclusion = result.alternative,
                            templateRaw = template.name,
                            providerRaw = providerRaw,
                            modelRaw = modelRaw,
                            existingMessagesJSON = followUpMessagesJSON,
                            viewModel = vm,
                            onDismiss = { showFollowUp = false }
                        )
                    }
                }
            }
        }

        // Custom Toast overlay is aligned to top center of the Box

        // Custom Toast overlay
        AnimatedVisibility(
            visible = copiedToast,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -40 }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Green,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("已复制到剪贴板", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ResultSection(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    isCollapsed: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }

        AnimatedVisibility(visible = !isCollapsed) {
            Column {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 36.dp)
                )
            }
        }
    }
}

@Composable
private fun SocraticQuestionsSection(
    result: AnalysisResult,
    isCollapsed: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QuestionMark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "引导问题",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }

        AnimatedVisibility(visible = !isCollapsed) {
            Column(modifier = Modifier.padding(start = 36.dp)) {
                Spacer(Modifier.height(8.dp))
                val qs = result.questions ?: emptyList()
                if (qs.isEmpty()) {
                    Text("（无问题列表）", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    qs.forEachIndexed { idx, q ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = "${idx + 1}.",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(20.dp),
                                fontSize = 14.sp
                            )
                            Text(
                                text = q,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DividerLine() {
    Divider(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        thickness = 1.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

private fun toggleSection(section: String, collapsed: Set<String>, onUpdate: (Set<String>) -> Unit) {
    if (collapsed.contains(section)) {
        onUpdate(collapsed - section)
    } else {
        onUpdate(collapsed + section)
    }
}

private fun getKeyInsightText(result: AnalysisResult, template: ThinkingTemplate): String? {
    val source = when (template) {
        ThinkingTemplate.cbt -> result.alternative
        ThinkingTemplate.socratic -> if (result.alternative.isEmpty()) result.action else result.alternative
        ThinkingTemplate.behavioral -> result.action
    }
    val sentence = source.split('。', '！', '？', '\n').firstOrNull { it.isNotBlank() }?.trim() ?: ""
    return if (sentence.isEmpty()) null else sentence
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
            val actions = result.actions
            if (!actions.isNullOrEmpty()) {
                text += "\n更多行动：${actions.joinToString("；")}"
            }

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
