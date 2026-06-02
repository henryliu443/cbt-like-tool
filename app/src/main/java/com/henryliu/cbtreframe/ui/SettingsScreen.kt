package com.henryliu.cbtreframe.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.henryliu.cbtreframe.shared.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    globalSettings: GlobalSettings = GlobalSettings.Default,
    onGlobalSettingsChange: (GlobalSettings) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showKeyField by remember { mutableStateOf(false) }
    var showDisableDisclaimerConfirm by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // ── API Key save + auto-refresh helper ──
    fun onSaveApiKey() {
        viewModel.saveAPIKey()
        // Snackbar feedback
        val msg = if (uiState.apiKeyInput.trim().isEmpty())
            "已清除 API Key"
        else
            "API Key 已保存"
        coroutineScope.launch {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                actions = {
                    if (uiState.isRefreshingModels) {
                        Row(
                            modifier = Modifier.padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "获取模型中",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                .verticalScroll(rememberScrollState())
        ) {
            // ── AI Provider Section (hide LOCAL from main list) ──
            SettingsSectionHeader("AI 服务商", Icons.Default.Settings)
            AIProvider.values().filter { it != AIProvider.LOCAL }.forEach { provider ->
                val isSelected = uiState.selectedProvider == provider
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectProvider(provider) },
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else
                        MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                provider.displayName(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                if (provider != AIProvider.values().last { it != AIProvider.LOCAL }) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── API Key Section ──
            if (uiState.selectedProvider.requiresApiKey()) {
                SettingsSectionHeader("API Key", Icons.Default.Lock)
                OutlinedTextField(
                    value = uiState.apiKeyInput,
                    onValueChange = { viewModel.updateApiKeyInput(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("粘贴你的 API Key") },
                    visualTransformation = if (showKeyField)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    singleLine = true,
                    trailingIcon = {
                        TextButton(onClick = { showKeyField = !showKeyField }) {
                            Text(if (showKeyField) "隐藏" else "显示", fontSize = 13.sp)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onSaveApiKey() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    enabled = !uiState.isSavingAPIKey,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isSavingAPIKey) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("保存 API Key")
                }

                Spacer(Modifier.height(16.dp))
            }

            // ── Model Section ──
            if (uiState.resolvedModels.isNotEmpty()) {
                SettingsSectionHeader("模型选择", Icons.Default.Build)
                uiState.resolvedModels.forEach { model ->
                    val isModelSelected = uiState.selectedModelId == model.modelName
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectModel(model.modelName) },
                        color = if (isModelSelected)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                model.modelName,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isModelSelected) FontWeight.Medium else FontWeight.Normal
                            )
                            if (isModelSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    if (model != uiState.resolvedModels.last()) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Model Fetch Error ──
            if (uiState.modelsListError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
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
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            uiState.modelsListError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { coroutineScope.launch { viewModel.refreshModels() } },
                        ) {
                            Text("重试", fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Security Section ──
            SettingsSectionHeader("安全与隐私", Icons.Default.Lock)
            if (viewModel.canUseBiometrics) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "面容 / 指纹解锁",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = uiState.useFaceID,
                        onCheckedChange = { viewModel.setUseFaceID(it) }
                    )
                }
            }

            // ── Reminder Section ──
            SettingsSectionHeader("每日提醒", Icons.Default.Notifications)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "开启每日提醒",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = uiState.dailyReminderEnabled,
                    onCheckedChange = { viewModel.setDailyReminderEnabled(it) }
                )
            }
            if (uiState.dailyReminderEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("提醒时间", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    NumberPickerButton(
                        value = uiState.reminderHour,
                        range = 0..23,
                        format = { String.format("%02d", it) },
                        onValueChange = { viewModel.setReminderHour(it) }
                    )
                    Text(":", style = MaterialTheme.typography.titleMedium)
                    NumberPickerButton(
                        value = uiState.reminderMinute,
                        range = 0..59,
                        format = { String.format("%02d", it) },
                        onValueChange = { viewModel.setReminderMinute(it) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Danger Zone ──
            SettingsSectionHeader("数据管理", Icons.Default.Delete)
            OutlinedButton(
                onClick = { showClearConfirmation = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "清除所有数据",
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Disclaimer Section ──
            SettingsSectionHeader("免责声明与服务协议", Icons.Default.Warning)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "我同意本应用免责声明与服务协议",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = uiState.hasAcceptedDisclaimer,
                    onCheckedChange = { checked ->
                        if (checked) viewModel.setHasAcceptedDisclaimer(true)
                        else showDisableDisclaimerConfirm = true
                    }
                )
            }
            Text(
                "若你处于危机中，请立即联系专业机构或急救服务。",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // ── About Section ──
            SettingsSectionHeader("关于", Icons.Default.Info)
            AboutRow("版本", "1.0")
            AboutRow("当前服务商", uiState.selectedProvider.displayName())
            AboutRow("当前模型", uiState.selectedModel.modelName)

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Dialogs ──

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("确认清除") },
            text = { Text("这将删除所有 API Key、历史记录和设置。此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        viewModel.clearAllData { /* clear database */ }
                        onGlobalSettingsChange(GlobalSettings.Default)
                    }
                ) {
                    Text("清除所有数据", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showDisableDisclaimerConfirm) {
        AlertDialog(
            onDismissRequest = { showDisableDisclaimerConfirm = false },
            title = { Text("确认关闭免责声明？") },
            text = { Text("关闭后将立即退出 App。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisableDisclaimerConfirm = false
                        viewModel.setHasAcceptedDisclaimer(false)
                    }
                ) {
                    Text("关闭并退出 App", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDisableDisclaimerConfirm = false
                    viewModel.setHasAcceptedDisclaimer(true)
                }) {
                    Text("继续开启")
                }
            }
        )
    }
}

// ── Private components ──

@Composable
private fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NumberPickerButton(
    value: Int,
    range: IntRange,
    format: (Int) -> String,
    onValueChange: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = {
                val next = if (value > range.first) value - 1 else range.last
                onValueChange(next)
            }
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "增加")
        }
        Text(format(value), style = MaterialTheme.typography.bodyLarge)
        IconButton(
            onClick = {
                val next = if (value < range.last) value + 1 else range.first
                onValueChange(next)
            }
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "减少")
        }
    }
}
