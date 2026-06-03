package com.henryliu.cbtreframe.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.henryliu.cbtreframe.shared.AnalysisResult
import com.henryliu.cbtreframe.shared.ThinkingTemplate

@Composable
fun ResultCardView(
    result: AnalysisResult,
    template: ThinkingTemplate = ThinkingTemplate.cbt,
    inputThought: String = "",
    moodTag: String = "",
    analysisDepthLabel: String = "",
    historyEntryID: String? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
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

            // Distortion
            if (result.distortion.isNotBlank()) {
                SectionLabel("认知扭曲")
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

            // State assessment
            val state = result.stateAssessment
            if (!state.isNullOrBlank()) {
                SectionLabel("状态评估")
                Spacer(Modifier.height(8.dp))
                Text(
                    state,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
            }

            // Alternative thought
            if (result.alternative.isNotBlank()) {
                SectionLabel("替代想法")
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Text(
                        result.alternative,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // Socratic questions
            val questions = result.questions
            if (!questions.isNullOrEmpty()) {
                SectionLabel("引导问题")
                Spacer(Modifier.height(8.dp))
                questions.forEach { q ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("• ", color = MaterialTheme.colorScheme.primary)
                        Text(q, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Action
            if (result.action.isNotBlank()) {
                SectionLabel("建议行动")
                Spacer(Modifier.height(8.dp))
                Text(result.action, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}
