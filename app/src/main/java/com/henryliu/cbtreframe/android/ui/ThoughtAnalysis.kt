package com.henryliu.cbtreframe.android.ui

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.henryliu.cbtreframe.shared.HomeStage
import com.henryliu.cbtreframe.shared.ThinkingTemplate
import com.henryliu.cbtreframe.ui.icon
import com.henryliu.cbtreframe.ui.label

@Composable
fun ThoughtInputCard(
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
            SectionHeader(
                icon = Icons.Default.ChatBubbleOutline,
                title = "写1句：现在最烦的事",
                accColor = accColor,
            )

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

@Composable
fun TemplatePicker(
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
            SectionHeader(
                icon = Icons.Default.AutoAwesome,
                title = "选最快开始的方式",
                accColor = accColor,
            )

            if (suggestedTemplate != null && suggestedTemplate != selectedTemplate) {
                SuggestionChip(
                    onClick = { onTemplateSelected(suggestedTemplate) },
                    label = {
                        Text(
                            "推荐: ${suggestedTemplate.label.replace("\n", "")}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                    },
                    icon = {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
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
private fun RowScope.TemplateCol(
    template: ThinkingTemplate,
    isSelected: Boolean,
    isSuggested: Boolean,
    accColor: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
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
                    imageVector = template.icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else accColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Text(
            template.label,
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

data class MoodTag(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String)

val sharedMoods = listOf(
    MoodTag(Icons.Default.SentimentDissatisfied, "\u4F4E\u843D"),
    MoodTag(Icons.Default.Warning, "\u7126\u8651"),
    MoodTag(Icons.Default.MoodBad, "\u6124\u6012"),
    MoodTag(Icons.Default.ErrorOutline, "\u62C5\u5FE7"),
    MoodTag(Icons.Default.SentimentVeryDissatisfied, "\u5931\u671B"),
    MoodTag(Icons.Default.Face, "\u75B2\u60EB"),
    MoodTag(Icons.Default.SentimentNeutral, "\u9EBB\u6728"),
    MoodTag(Icons.Default.Psychology, "\u5185\u5728\u4E0D\u5B89"),
    MoodTag(Icons.Default.SentimentVerySatisfied, "\u5F00\u5FC3"),
    MoodTag(Icons.Default.SentimentSatisfied, "\u6109\u5FEB")
)

@Composable
fun MoodPicker(
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
            SectionHeader(
                icon = Icons.Default.FavoriteBorder,
                title = "点选当前心情",
                accColor = accColor,
                trailingContent = {
                    Text(
                        "必选",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = accColor,
                        modifier = Modifier
                            .background(accColor.copy(alpha = 0.12f), RoundedCornerShape(50))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
            )

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
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(mood.icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isSel) accColor else MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
fun AnalyzeBtn(
    homeStage: HomeStage,
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
                        imageVector = if (homeStage.order >= HomeStage.ChoosingMood.order)
                            Icons.Default.Refresh else Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buttonTitle(homeStage, isLoading),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
        }
    }
}

private fun buttonTitle(stage: HomeStage, loading: Boolean): String {
    if (loading) return "正在分析…"
    return when (stage) {
        HomeStage.QuickStart, HomeStage.WritingThought -> "下一步：选最省力的方式"
        HomeStage.ChoosingMode -> "下一步：点当前心情"
        HomeStage.ChoosingMood, HomeStage.ReviewReady -> "开始分析"
    }
}
