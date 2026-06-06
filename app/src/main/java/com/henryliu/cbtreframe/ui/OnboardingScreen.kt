package com.henryliu.cbtreframe.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.henryliu.cbtreframe.shared.AIProvider
import com.henryliu.cbtreframe.shared.SettingsViewModel
import com.henryliu.cbtreframe.shared.requiresApiKey
import kotlinx.coroutines.launch

// ── Onboarding flow ──────────────────────────────────────────────────

/**
 * Three-step onboarding displayed only when the user has not yet accepted
 * the disclaimer.  After completing all steps the caller sets
 * `onOnboardingComplete` which ultimately calls
 * [SettingsViewModel.setHasAcceptedDisclaimer] and persists the flag.
 */
@Composable
fun OnboardingScreen(
    viewModel: SettingsViewModel,
    onOnboardingComplete: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 3

    // Local state for step 2 (provider + api key)
    var selectedProvider by remember { mutableStateOf(uiState.selectedProvider) }
    var apiKey by remember { mutableStateOf(uiState.apiKeyInput) }
    var showKey by remember { mutableStateOf(false) }

    // Step 3 disclaimer acceptance
    var disclaimerAccepted by remember { mutableStateOf(false) }

    // Initialization states
    var isInitializing by remember { mutableStateOf(false) }
    var initializationError by remember { mutableStateOf<String?>(null) }

    val runInitialization = {
        isInitializing = true
        initializationError = null
        coroutineScope.launch {
            val result = viewModel.initializeOnboarding(selectedProvider, apiKey)
            if (result.isSuccess) {
                onOnboardingComplete()
            } else {
                initializationError = result.exceptionOrNull()?.message ?: "Unknown error"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isInitializing) {
            InitializationOverlay(
                error = initializationError,
                onRetry = {
                    isInitializing = true
                    initializationError = null
                    coroutineScope.launch {
                        val result = viewModel.initializeOnboarding(selectedProvider, apiKey)
                        if (result.isSuccess) {
                            onOnboardingComplete()
                        } else {
                            initializationError = result.exceptionOrNull()?.message ?: "Unknown error"
                        }
                    }
                },
                onSwitchToLocal = {
                    coroutineScope.launch {
                        viewModel.initializeOnboarding(AIProvider.LOCAL, "")
                        onOnboardingComplete()
                    }
                },
                onBackToConfig = {
                    isInitializing = false
                    initializationError = null
                    currentStep = 1
                }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Progress indicator ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { if (currentStep > 0) currentStep-- },
                        enabled = currentStep > 0,
                    ) {
                        Text(if (currentStep > 0) "← 上一步" else "")
                    }

                    Spacer(Modifier.weight(1f))

                    repeat(totalSteps) { idx ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (idx <= currentStep) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    TextButton(
                        onClick = {
                            if (currentStep < totalSteps - 1) {
                                currentStep++
                            } else {
                                runInitialization()
                            }
                        },
                        enabled = when (currentStep) {
                            0 -> true  // any next from welcome
                            1 -> selectedProvider == AIProvider.LOCAL ||
                                 (selectedProvider.requiresApiKey() && apiKey.isNotBlank())
                            2 -> disclaimerAccepted
                            else -> false
                        },
                    ) {
                        Text(if (currentStep < totalSteps - 1) "下一步 →" else "完成 ✓")
                    }
                }

                // ── Horizontal pager (manual) ──
                AnimatedContent(
                    targetState = currentStep,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        slideInHorizontally { width -> dir * width } togetherWith
                            slideOutHorizontally { width -> -dir * width }
                    },
                    label = "onboarding-step",
                ) { step ->
                    when (step) {
                        0 -> StepWelcome()
                        1 -> StepProviderAndKey(
                            selectedProvider = selectedProvider,
                            onProviderChanged = { provider ->
                                selectedProvider = provider
                                if (!provider.requiresApiKey()) {
                                    apiKey = ""
                                }
                            },
                            apiKey = apiKey,
                            onApiKeyChanged = { apiKey = it },
                            showKey = showKey,
                            onToggleShowKey = { showKey = !showKey },
                        )
                        2 -> StepDisclaimer(
                            accepted = disclaimerAccepted,
                            onAcceptChanged = { disclaimerAccepted = it },
                            onComplete = {
                                runInitialization()
                            },
                            canComplete = disclaimerAccepted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InitializationOverlay(
    error: String?,
    onRetry: () -> Unit,
    onSwitchToLocal: () -> Unit,
    onBackToConfig: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (error != null) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "初始化未完成",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            Text(
                error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("重试同步", fontSize = 16.sp)
            }

            Spacer(Modifier.height(12.dp))

            FilledTonalButton(
                onClick = onSwitchToLocal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("切换为本地离线模式", fontSize = 15.sp)
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onBackToConfig) {
                Text("返回修改 API Key", color = MaterialTheme.colorScheme.primary)
            }
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "正在初始化应用环境...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "正在保存服务商与 API Key 配置",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "正在从服务商同步模型列表",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Step 1: Welcome ─────────────────────────────────────────────────

@Composable
private fun StepWelcome() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "🧠",
            fontSize = 64.sp,
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "欢迎使用 CBT Reframe",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "认知行为疗法的自助辅助工具\n\n帮助你识别和重塑消极思维模式",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))

        OnboardingFeatureRow(Icons.Default.Psychology, "识别认知扭曲")
        OnboardingFeatureRow(Icons.Default.Lightbulb, "生成替代想法")
        OnboardingFeatureRow(Icons.Default.CheckCircle, "制定行动步骤")
        OnboardingFeatureRow(Icons.Default.Security, "数据本地存储")

        Spacer(Modifier.height(32.dp))

        Text(
            "点击右上角「下一步」开始设置 →",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OnboardingFeatureRow(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Step 2: Provider + API Key ──────────────────────────────────────

@Composable
private fun StepProviderAndKey(
    selectedProvider: AIProvider,
    onProviderChanged: (AIProvider) -> Unit,
    apiKey: String,
    onApiKeyChanged: (String) -> Unit,
    showKey: Boolean,
    onToggleShowKey: () -> Unit,
) {
    val selectableProviders = AIProvider.values().filter { it != AIProvider.LOCAL }
    val isLocal = selectedProvider == AIProvider.LOCAL || !selectedProvider.requiresApiKey()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            "选择 AI 服务商",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "选择一个 AI 服务商并输入对应的 API Key。\n你也可以选择「本地」模式离线使用（功能有限）。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))

        // ── Provider list (excluding LOCAL) ──
        selectableProviders.forEach { provider ->
            val isSelected = selectedProvider == provider
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onProviderChanged(provider) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else
                    MaterialTheme.colorScheme.surface,
                tonalElevation = if (isSelected) 2.dp else 0.dp,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            provider.displayName(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        )
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        // ── Local option ──
        val localSelected = selectedProvider == AIProvider.LOCAL
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onProviderChanged(AIProvider.LOCAL) },
            shape = RoundedCornerShape(12.dp),
            color = if (localSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surface,
            tonalElevation = if (localSelected) 2.dp else 0.dp,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        AIProvider.LOCAL.displayName(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (localSelected) FontWeight.Medium else FontWeight.Normal,
                    )
                    Text(
                        "无需 API Key，离线可用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (localSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── API Key field (hidden for LOCAL) ──
        AnimatedVisibility(visible = selectedProvider != AIProvider.LOCAL) {
            Column {
                Text(
                    "API Key",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(Modifier.height(6.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入你的 API Key") },
                    singleLine = true,
                    visualTransformation = if (showKey)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = onToggleShowKey) {
                            Icon(
                                if (showKey) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = if (showKey) "隐藏" else "显示",
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "你的 API Key 仅存储在本机安全区域，不会上传。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (selectedProvider != AIProvider.LOCAL && apiKey.isBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "请输入 API Key 后方可继续",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

// ── Step 3: Disclaimer ──────────────────────────────────────────────

@Composable
private fun StepDisclaimer(
    accepted: Boolean,
    onAcceptChanged: (Boolean) -> Unit,
    onComplete: () -> Unit,
    canComplete: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "重要免责声明",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
        )

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DisclaimerItem("⚠️ 本应用不能替代专业心理咨询或医疗诊断。")
                Spacer(Modifier.height(12.dp))
                DisclaimerItem("⚠️ AI 生成的内容可能存在偏差、错误或不恰当的回复。")
                Spacer(Modifier.height(12.dp))
                DisclaimerItem("⚠️ 如果你正处于严重的心理危机或有自伤/自杀念头，请立即拨打心理援助热线。")
                Spacer(Modifier.height(12.dp))
                DisclaimerItem("📞 心理援助热线：400-161-9995")
                DisclaimerItem("📞 北京心理危机干预中心：010-82951332")
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = accepted,
                onCheckedChange = onAcceptChanged,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "我已阅读并理解上述声明",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { onAcceptChanged(!accepted) },
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = canComplete,
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("进入应用", fontSize = 16.sp)
        }

        if (!canComplete) {
            Spacer(Modifier.height(8.dp))
            Text(
                "请先勾选以确认你已阅读并理解免责声明",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DisclaimerItem(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
        lineHeight = 22.sp,
    )
}
