package com.henryliu.cbtreframe.ui

import androidx.compose.animation.*
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
// M3 Expressive HomeScreen
// Organic, fluid, warm — atmospheric greeting + blur,
// asymmetric dashboard, expressive input, spring
// chips, breathing elliptical CTA.
// ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ReframeViewModel,
    globalSettings: GlobalSettings = GlobalSettings.Default,
    onGlobalSettingsChange: (GlobalSettings) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var isInputFocused by remember { mutableStateOf(false) }
    var showSecondaryTools by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Breathing glow animation for the Analyze button
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val breatheAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheAlpha"
    )
    val breatheRadius by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheRadius"
    )

    // Atmospheric gradient — shifts hue subtly with scroll offset
    val gradientColors = remember {
        listOf(
            Color(0xFFFFF3E0), // warm cream
            Color(0xFFFFECB3), // soft amber
            Color(0xFFFFF8E1)  // light lemon
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Atmospheric background layer ──
        AtmosphericBackground(gradientColors = gradientColors)

        // ── Foreground scrollable content ──
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

                // ── Atmospheric Greeting ──
                AtmosphericGreeting(uiState = uiState)

                // ── Asymmetric Today Dashboard ──
                AsymmetricDashboard(uiState = uiState)

                if (uiState.result != null) {
                    OutlinedButton(
                        onClick = { viewModel.reset() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("开始新的分析")
                    }
                }

                // ── Expressive Fluid Thought Input ──
                FluidThoughtInput(
                    text = uiState.inputText,
                    onTextChange = { viewModel.setInputText(it) },
                    isFocused = isInputFocused,
                    onFocusChanged = { isInputFocused = it }
                )

                // ── Spring-Animated Mood Chips ──
                AnimatedVisibility(
                    visible = uiState.inputText.isNotBlank() || uiState.result != null,
                    enter = fadeIn(animationSpec = tween(400)) +
                        expandVertically(animationSpec = spring(dampingRatio = 0.7f)),
                    exit = fadeOut(animationSpec = tween(300)) +
                        shrinkVertically(animationSpec = spring(dampingRatio = 0.7f))
                ) {
                    SpringMoodChips(
                        selectedMood = uiState.selectedMood,
                        onMoodSelected = { viewModel.setSelectedMood(it) },
                        isAkathisia = uiState.isAkathisia,
                        onAkathisiaToggle = { viewModel.setAkathisia(it) }
                    )
                }

                // ── Breathing Elliptical Analyze Button ──
                BreathingAnalyzeButton(
                    isLoading = uiState.isLoading,
                    enabled = uiState.inputText.isNotBlank() && !uiState.isLoading,
                    breatheAlpha = breatheAlpha,
                    breatheRadius = breatheRadius,
                    inputText = uiState.inputText,
                    selectedMood = uiState.selectedMood,
                    onClick = { viewModel.analyzeThought(globalSettings) }
                )

                if (uiState.inputText.isNotBlank() || uiState.result != null) {
                    SecondaryToolsSection(
                        expanded = showSecondaryTools,
                        onToggle = { showSecondaryTools = !showSecondaryTools },
                        viewModel = viewModel,
                        uiState = uiState
                    )
                }

                if (uiState.inputText.isBlank() && uiState.result == null) {
                    QuickStartSection(viewModel)
                }

                AnimatedVisibility(
                    visible = uiState.isLoading && uiState.loadingBannerStyle != LoadingBannerStyle.NONE
                ) {
                    LoadingBanner(uiState)
                }

                AnimatedVisibility(visible = uiState.retryRecoveryNotice != null) {
                    uiState.retryRecoveryNotice?.let { notice ->
                        RetryRecoveryBanner(notice)
                    }
                }

                AnimatedVisibility(visible = uiState.showCrisisBanner) {
                    SafetyBannerView()
                }

                uiState.errorMessage?.let { error ->
                    ErrorBanner(error)
                }

                AnimatedVisibility(visible = uiState.result != null) {
                    uiState.result?.let { result ->
                        ResultCardScreen(
                            result = result,
                            template = globalSettings.thinkingTemplate,
                            inputThought = uiState.inputText,
                            moodTag = uiState.selectedMood,
                            analysisDepthLabel = globalSettings.analysisDepth.displayName(),
                            historyEntryID = uiState.latestHistoryEntryID
                        )
                    }
                }

                if (uiState.isStreamingResult && uiState.errorMessage == null) {
                    StreamingResultCard(uiState.streamingText)
                }

                Spacer(Modifier.height(60.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// 1. ATMOSPHERIC BACKGROUND
// ═══════════════════════════════════════════════════

@Composable
private fun AtmosphericBackground(gradientColors: List<Color>) {
    // Animated time-based color shift
    val infiniteTransition = rememberInfiniteTransition(label = "atmosphereHue")
    val hueShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hueShift"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Large soft radial gradient blobs — atmospheric color wash
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

// ═══════════════════════════════════════════════════
// 2. ATMOSPHERIC GREETING HEADER
// ═══════════════════════════════════════════════════

@Composable
private fun AtmosphericGreeting(uiState: ReframeUiState) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val greetingOffset by animateDpAsState(
        targetValue = if (visible) 0.dp else 24.dp,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "greetingSlide"
    )
    val greetingAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "greetingFade"
    )

    // Blurred glass-morphism surface behind greeting
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = greetingAlpha
                translationY = greetingOffset.toPx()
            }
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(topStart = 28.dp, bottomEnd = 28.dp)
            )
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = uiState.greeting,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = uiState.todayQuote,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════
// 3. ASYMMETRIC TODAY DASHBOARD
// ═══════════════════════════════════════════════════

@Composable
private fun AsymmetricDashboard(uiState: ReframeUiState) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val dashboardScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dashboardScale"
    )

    val dashboardAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, delayMillis = 150, easing = EaseOutCubic),
        label = "dashboardFade"
    )

    // Define unique shapes per card for asymmetry
    val streakShape = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 8.dp,
        bottomStart = 12.dp,
        bottomEnd = 24.dp
    )
    val longestShape = RoundedCornerShape(
        topStart = 8.dp,
        topEnd = 24.dp,
        bottomStart = 24.dp,
        bottomEnd = 12.dp
    )
    val todayShape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = 8.dp,
        bottomEnd = 24.dp
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = dashboardAlpha
                scaleX = dashboardScale
                scaleY = dashboardScale
            },
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Card 1: Current Streak — tall, warm accent
        AsymmetricDashboardCard(
            modifier = Modifier.weight(1.2f),
            shape = streakShape,
            gradientColors = listOf(
                Color(0xFFFFAB91),
                Color(0xFFFFCC80)
            ),
            icon = Icons.Default.Favorite,
            label = "当前连续",
            value = "${uiState.currentStreak} 天",
            heightMultiplier = 1.15f
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 2: Longest Streak — compact
            AsymmetricDashboardCard(
                modifier = Modifier.fillMaxWidth(),
                shape = longestShape,
                gradientColors = listOf(
                    Color(0xFFCE93D8),
                    Color(0xFF80CBC4)
                ),
                icon = Icons.Default.Star,
                label = "最长连续",
                value = "${uiState.longestStreak} 天",
                heightMultiplier = 1f
            )

            // Card 3: Today Analysis — compact
            AsymmetricDashboardCard(
                modifier = Modifier.fillMaxWidth(),
                shape = todayShape,
                gradientColors = listOf(
                    Color(0xFF90CAF9),
                    Color(0xFFA5D6A7)
                ),
                icon = Icons.Default.Edit,
                label = "今日分析",
                value = "${uiState.todayAnalysisCount} 次",
                heightMultiplier = 1f
            )
        }
    }
}

@Composable
private fun AsymmetricDashboardCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape,
    gradientColors: List<Color>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    heightMultiplier: Float
) {
    val baseHeight = 82.dp
    Box(
        modifier = modifier
            .height(baseHeight * heightMultiplier)
            .background(
                brush = Brush.linearGradient(
                    colors = gradientColors.map { it.copy(alpha = 0.35f) }
                ),
                shape = shape
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = gradientColors.map { it.copy(alpha = 0.5f) }
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = label,
                tint = gradientColors.first().copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═══════════════════════════════════════════════════
// 4. EXPRESSIVE FLUID THOUGHT INPUT
// ═══════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FluidThoughtInput(
    text: String,
    onTextChange: (String) -> Unit,
    isFocused: Boolean,
    onFocusChanged: (Boolean) -> Unit
) {
    // Animated border — fluid breathing when focused
    val infiniteTransition = rememberInfiniteTransition(label = "fluidBorder")
    val borderPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderPhase"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.dp else 1.dp,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
        label = "borderWidth"
    )

    val elevation by animateDpAsState(
        targetValue = if (isFocused) 8.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "inputElevation"
    )

    // Fluid shape — asymmetric organic rounded corners
    val fluidShape = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 8.dp,
        bottomStart = 12.dp,
        bottomEnd = 24.dp
    )

    // Rotating border gradient colors
    val borderStart = remember(borderPhase) {
        lerp(
            Color(0xFFFFAB91),
            Color(0xFFCE93D8),
            borderPhase
        )
    }
    val borderMid = remember(borderPhase) {
        lerp(
            Color(0xFF80CBC4),
            Color(0xFFFFAB91),
            borderPhase
        )
    }
    val borderEnd = remember(borderPhase) {
        lerp(
            Color(0xFF90CAF9),
            Color(0xFF80CBC4),
            borderPhase
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = elevation, shape = fluidShape, clip = false)
            .border(
                width = borderWidth,
                brush = Brush.sweepGradient(
                    colors = listOf(borderStart, borderMid, borderEnd, borderStart)
                ),
                shape = fluidShape
            ),
        shape = fluidShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        )
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .onFocusChanged { onFocusChanged(it.isFocused) },
            placeholder = {
                Text(
                    "写下你此刻的自动想法…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { onFocusChanged(false) }
            ),
            maxLines = 6,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 26.sp,
                letterSpacing = 0.3.sp
            )
        )
    }
}

// ═══════════════════════════════════════════════════
// 5. SPRING-ANIMATED MOOD CHIPS
// ═══════════════════════════════════════════════════

@Composable
private fun SpringMoodChips(
    selectedMood: String,
    onMoodSelected: (String) -> Unit,
    isAkathisia: Boolean,
    onAkathisiaToggle: (Boolean) -> Unit
) {
    val moods = listOf(
        "😊" to "开心", "😢" to "难过", "😤" to "生气",
        "😰" to "焦虑", "😴" to "疲惫", "🤔" to "困惑",
        "😐" to "平静", "🫠" to "烦躁", "😔" to "低落"
    )

    // Staggered reveal with spring
    val visibleCount by animateIntAsState(
        targetValue = moods.size,
        animationSpec = tween(
            durationMillis = 500 + moods.size * 80,
            easing = LinearEasing
        ),
        label = "chipCount"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "当前心情",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(moods.size) { index ->
                    val (emoji, label) = moods[index]
                    val isSelected = selectedMood == label

                    // Each chip springs in individually
                    SpringChip(
                        index = index,
                        isVisible = index < visibleCount,
                        emoji = emoji,
                        label = label,
                        isSelected = isSelected,
                        onClick = { onMoodSelected(if (isSelected) "" else label) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onAkathisiaToggle(!isAkathisia) }
            ) {
                Checkbox(
                    checked = isAkathisia,
                    onCheckedChange = { onAkathisiaToggle(it) }
                )
                Text(
                    "有静坐不能/坐立不安感",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpringChip(
    index: Int,
    isVisible: Boolean,
    emoji: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val springScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = 0.01f
        ),
        label = "chipScale"
    )
    val springAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(350, delayMillis = index * 55, easing = EaseOutBack),
        label = "chipAlpha"
    )

    val selectedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "chipSelectedScale"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            alpha = springAlpha
            scaleX = springScale * selectedScale
            scaleY = springScale * selectedScale
        }
    ) {
        FilterChip(
            selected = isSelected,
            onClick = onClick,
            label = { Text("$emoji $label", fontSize = 13.sp) },
            shape = RoundedCornerShape(12.dp),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }
}

// ═══════════════════════════════════════════════════
// 6. BREATHING ELLIPTICAL ANALYZE BUTTON
// ═══════════════════════════════════════════════════

@Composable
private fun BreathingAnalyzeButton(
    isLoading: Boolean,
    enabled: Boolean,
    breatheAlpha: Float,
    breatheRadius: Float,
    inputText: String,
    selectedMood: String,
    onClick: () -> Unit
) {
    val glowColors = remember {
        listOf(
            Color(0xFFFF8A65),
            Color(0xFFFFAB91).copy(alpha = 0.6f),
            Color(0xFFFFCC80).copy(alpha = 0.3f),
            Color.Transparent
        )
    }

    // Determine contextual button label
    val label = when {
        isLoading -> "分析中…"
        inputText.isBlank() -> "下一步：选最省力的方式"
        selectedMood.isBlank() -> "下一步：点当前心情"
        else -> "开始分析"
    }

    // Spring press animation
    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessHigh),
        label = "pressScale"
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing halo
        Canvas(
            modifier = Modifier
                .width(220.dp)
                .height(68.dp)
        ) {
            drawOval(
                brush = Brush.radialGradient(
                    colors = glowColors,
                    center = Offset(size.width / 2, size.height / 2),
                    radius = size.width * 0.55f * breatheRadius
                ),
                alpha = breatheAlpha
            )
        }

        // The button itself — oversized, elliptical
        Button(
            onClick = onClick,
            modifier = Modifier
                .width(200.dp)
                .height(56.dp)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                },
            enabled = enabled,
            shape = RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 28.dp,
                bottomStart = 28.dp,
                bottomEnd = 28.dp
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF8A65),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFFFAB91).copy(alpha = 0.4f),
                disabledContentColor = Color.White.copy(alpha = 0.5f)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp,
                disabledElevation = 0.dp
            ),
            interactionSource = remember { MutableInteractionSource() }
                .also { source ->
                    LaunchedEffect(source) {
                        source.interactions.collect { interaction ->
                            isPressed = interaction is androidx.compose.foundation.interaction.PressInteraction
                        }
                    }
                }
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════
// RETAINED: Quick Start, Secondary Tools, Loading,
// Safety, Error, Streaming, Retry — unchanged from
// original to preserve full functionality.
// ═══════════════════════════════════════════════════

// ── Quick Start Section ──

@Composable
private fun QuickStartSection(viewModel: ReframeViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "快速开始",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ReframeViewModel.quickStartPrompts.forEach { (emoji, prompt) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setInputText(prompt) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(emoji, fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ── Secondary Tools Section ──

@Composable
private fun SecondaryToolsSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    viewModel: ReframeViewModel,
    uiState: ReframeUiState
) {
    Column {
        TextButton(onClick = onToggle) {
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("更多选项", fontSize = 14.sp)
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "使用外部 AI App",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "复制提示词到 ChatGPT / Gemini / Kimi 等 App",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            /* Clipboard handled by platform */
                        }) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "复制提示词",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Loading Banner ──

@Composable
private fun LoadingBanner(uiState: ReframeUiState) {
    val isGemini = uiState.loadingBannerStyle == LoadingBannerStyle.GEMINI_PRO

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGemini)
                Color(0xFF1A73E8).copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    uiState.currentThinkingPhrase,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "已耗时 ${uiState.analysisElapsedSeconds} 秒",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Retry Recovery Banner ──

@Composable
private fun RetryRecoveryBanner(notice: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                notice,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF2E7D32)
            )
        }
    }
}

// ── Safety Banner ──

@Composable
private fun SafetyBannerView() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "安全提示",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "此AI分析仅供参考，不能替代专业心理咨询。\n如果你正在经历严重的情绪困扰或危机，请立即联系专业人士。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
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

// ── Error Banner ──

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

// ── Streaming Result Card ──

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
