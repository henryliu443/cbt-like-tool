package com.henryliu.cbtreframe.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.henryliu.cbtreframe.shared.AnalysisResult
import com.henryliu.cbtreframe.shared.ThinkingTemplate

@Composable
fun ResultCardScreen(
    result: AnalysisResult,
    template: ThinkingTemplate = ThinkingTemplate.cbt,
    inputThought: String = "",
    moodTag: String = "",
    analysisDepthLabel: String = "",
    historyEntryID: String? = null,
) {
    var copiedToast by remember { mutableStateOf(false) }
    var showFollowUp by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    // We apply a simple reveal animation on appearance
    var reveal by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { reveal = true }

    val normalized = result.normalized(template)

    Box {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            AnimatedVisibility(visible = reveal) {
                Column {
                    // ── Key Insight Header ──
                    val insight = computeKeyInsight(normalized, template)
                    if (insight != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            tonalElevation = 1.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "关键提醒",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    insight,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }

                    // ── Template-specific content ──
                    when (template) {
                        ThinkingTemplate.cbt -> CbtContent(normalized)
                        ThinkingTemplate.socratic -> SocraticContent(normalized)
                        ThinkingTemplate.behavioral -> BehavioralContent(normalized)
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // ── Action Bar ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = {
                            val text = buildCopyText(normalized, inputThought, moodTag, template)
                            clipboardManager.setText(AnnotatedString(text))
                            copiedToast = true
                        }) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "复制",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { /* share - platform specific */ }) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "分享",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { showFollowUp = true }) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "继续探索",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // ── Continue Exploration Footer ──
                    TextButton(
                        onClick = { showFollowUp = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("继续探索", fontSize = 14.sp)
                    }
                }
            }
        }

        // ── Copied Toast ──
        AnimatedVisibility(
            visible = copiedToast,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 3.dp,
                modifier = Modifier.offset(y = (-12).dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "已复制到剪贴板",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        LaunchedEffect(copiedToast) {
            if (copiedToast) {
                kotlinx.coroutines.delay(2000)
                copiedToast = false
            }
        }
    }
}

// ── CBT Content ──

@Composable
private fun CbtContent(result: AnalysisResult) {
    ResultSection(
        icon = Icons.Default.Warning,
        iconTint = Color(0xFFFF9800),
        label = "认知扭曲",
        value = result.distortion
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    ResultSection(
        icon = Icons.Default.Build,
        iconTint = MaterialTheme.colorScheme.primary,
        label = "替代想法",
        value = result.alternative
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    ResultSection(
        icon = Icons.Default.Person,
        iconTint = Color(0xFF4CAF50),
        label = "建议行动",
        value = result.action
    )
    if (!result.actions.isNullOrEmpty()) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        ResultSection(
            icon = Icons.Default.List,
            iconTint = Color(0xFF009688),
            label = "更多行动",
            value = result.actions?.joinToString("\n") ?: ""
        )
    }
}

// ── Socratic Content ──

@Composable
private fun SocraticContent(result: AnalysisResult) {
    if (result.distortion.isNotBlank()) {
        ResultSection(
            icon = Icons.Default.Info,
            iconTint = Color(0xFFFF9800),
            label = "视角提示",
            value = result.distortion
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
    SocraticQuestionsSection(result)
    if (result.alternative.isNotBlank()) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        ResultSection(
            icon = Icons.Default.Send,
            iconTint = MaterialTheme.colorScheme.primary,
            label = "参考方向",
            value = result.alternative
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    ResultSection(
        icon = Icons.Default.Edit,
        iconTint = Color(0xFF4CAF50),
        label = "下一步行动",
        value = result.action
    )
}

@Composable
private fun SocraticQuestionsSection(result: AnalysisResult) {
    val questions = result.questions ?: emptyList()
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "引导问题",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(10.dp))
        questions.forEachIndexed { index, q ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "${index + 1}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    q,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ── Behavioral Content ──

@Composable
private fun BehavioralContent(result: AnalysisResult) {
    ResultSection(
        icon = Icons.Default.Info,
        iconTint = Color(0xFFFF9800),
        label = "行为聚焦",
        value = result.distortion
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    ResultSection(
        icon = Icons.Default.Favorite,
        iconTint = MaterialTheme.colorScheme.primary,
        label = "积极视角",
        value = result.alternative
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    ResultSection(
        icon = Icons.Default.PlayArrow,
        iconTint = Color(0xFF4CAF50),
        label = "下一步行动",
        value = result.action
    )
    if (!result.actions.isNullOrEmpty()) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        ResultSection(
            icon = Icons.Default.Check,
            iconTint = Color(0xFF009688),
            label = "可选步骤",
            value = result.actions?.joinToString("\n") ?: ""
        )
    }
    val state = result.stateAssessment?.takeIf { it.isNotBlank() }
    if (state != null) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "状态评估",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                state,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ── Generic Result Section ──

@Composable
private fun ResultSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Helpers ──

private fun computeKeyInsight(result: AnalysisResult, template: ThinkingTemplate): String? {
    return when (template) {
        ThinkingTemplate.cbt -> result.distortion.takeIf { it.isNotBlank() && it != "未识别" }
        ThinkingTemplate.socratic -> {
            result.questions?.firstOrNull()?.takeIf { it.isNotBlank() }
                ?: result.distortion.takeIf { it.isNotBlank() && it != "未识别" && it != "苏格拉底提问" }
        }
        ThinkingTemplate.behavioral -> result.stateAssessment?.takeIf { it.isNotBlank() }
            ?: result.distortion.takeIf { it.isNotBlank() && it != "未识别" && it != "行为聚焦" }
    }
}

private fun buildCopyText(
    result: AnalysisResult,
    inputThought: String,
    moodTag: String,
    template: ThinkingTemplate,
): String {
    val sb = StringBuilder()
    if (inputThought.isNotBlank()) {
        sb.appendLine("原始想法：$inputThought")
    }
    if (moodTag.isNotBlank()) {
        sb.appendLine("心情标签：$moodTag")
    }
    sb.appendLine()
    when (template) {
        ThinkingTemplate.cbt -> {
            sb.appendLine("认知扭曲：${result.distortion}")
            sb.appendLine("替代想法：${result.alternative}")
            sb.appendLine("建议行动：${result.action}")
        }
        ThinkingTemplate.socratic -> {
            sb.appendLine("视角提示：${result.distortion}")
            if (!result.questions.isNullOrEmpty()) {
                sb.appendLine("引导问题：")
                result.questions?.forEachIndexed { i, q ->
                    sb.appendLine("  ${i + 1}. $q")
                }
            }
            sb.appendLine("参考方向：${result.alternative}")
            sb.appendLine("下一步行动：${result.action}")
        }
        ThinkingTemplate.behavioral -> {
            sb.appendLine("行为聚焦：${result.distortion}")
            sb.appendLine("积极视角：${result.alternative}")
            sb.appendLine("下一步行动：${result.action}")
        }
    }
    return sb.toString().trimEnd()
}
