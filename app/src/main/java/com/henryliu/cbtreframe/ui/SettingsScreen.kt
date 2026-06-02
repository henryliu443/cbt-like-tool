package com.henryliu.cbtreframe.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.henryliu.cbtreframe.shared.AIProvider
import com.henryliu.cbtreframe.shared.GlobalSettings
import com.henryliu.cbtreframe.shared.SettingsViewModel
import com.henryliu.cbtreframe.shared.ThinkingTemplate.AnalysisDepth
import com.henryliu.cbtreframe.shared.ThinkingTemplate.AppResponseStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    globalSettings: GlobalSettings,
    onGlobalSettingsChange: (GlobalSettings) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showDisableDisclaimerConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-fetch models when API key is saved and updated
    LaunchedEffect(uiState.hasAPIKey) {
        if (uiState.hasAPIKey && uiState.selectedProvider != AIProvider.LOCAL) {
            viewModel.refreshModels()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            
            // ── AI Provider ──
            SettingsSectionHeader("AI 服务商")
            InsetGroup {
                AIProvider.entries.forEach { provider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectProvider(provider) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(provider.displayName(), modifier = Modifier.weight(1f))
                        if (uiState.selectedProvider == provider) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (provider != AIProvider.entries.last()) {
                        InsetDivider()
                    }
                }
            }

            // ── API Key & Model ──
            if (uiState.selectedProvider != AIProvider.LOCAL) {
                SettingsSectionHeader("API 设置")
                InsetGroup {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = uiState.apiKeyInput,
                            onValueChange = { viewModel.updateApiKeyInput(it) },
                            label = { Text("API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { 
                                    viewModel.saveAPIKey()
                                    Toast.makeText(context, "API Key 已保存", Toast.LENGTH_SHORT).show()
                                },
                                enabled = !uiState.isSavingAPIKey
                            ) {
                                Text(if (uiState.isSavingAPIKey) "保存中..." else "保存 Key")
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))

            SettingsSectionHeader("模型选择")
            InsetGroup {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (uiState.selectedProvider != AIProvider.LOCAL && uiState.hasAPIKey) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("可用模型", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = { coroutineScope.launch { viewModel.refreshModels() } },
                                enabled = !uiState.isRefreshingModels
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("刷新模型列表")
                            }
                        }
                    }
                    
                    if (uiState.resolvedModels.isEmpty()) {
                        Text("无可用模型", color = MaterialTheme.colorScheme.error)
                    } else {
                        uiState.resolvedModels.forEach { model ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectModel(model.modelName) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.selectedModelId == model.modelName,
                                    onClick = { viewModel.selectModel(model.modelName) }
                                )
                                Text(model.modelName, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // ── Preferences ──
            SettingsSectionHeader("偏好设置")
            InsetGroup {
                // Analysis Depth
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("分析深度", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        AnalysisDepth.entries.forEach { depth ->
                            val isSelected = globalSettings.analysisDepth == depth
                            FilterChip(
                                selected = isSelected,
                                onClick = { onGlobalSettingsChange(globalSettings.copy(analysisDepth = depth)) },
                                label = { Text(depth.name) }
                            )
                        }
                    }
                }
                InsetDivider()
                // Response Style
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("回应风格", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        AppResponseStyle.entries.forEach { style ->
                            val isSelected = globalSettings.responseStyle == style
                            FilterChip(
                                selected = isSelected,
                                onClick = { onGlobalSettingsChange(globalSettings.copy(responseStyle = style)) },
                                label = { Text(style.name) }
                            )
                        }
                    }
                }
            }
            
            // ── Prompt Template / Thinking Template ──
            SettingsSectionHeader("Prompt / 思考模板")
            InsetGroup {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("思考模板", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        com.henryliu.cbtreframe.shared.ThinkingTemplate.entries.forEach { template ->
                            val isSelected = globalSettings.thinkingTemplate == template
                            FilterChip(
                                selected = isSelected,
                                onClick = { onGlobalSettingsChange(globalSettings.copy(thinkingTemplate = template)) },
                                label = { Text(template.name) }
                            )
                        }
                    }
                }
            }
            
            // ── Privacy & Security ──
            SettingsSectionHeader("隐私与安全")
            InsetGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("历史记录保护 (Biometrics)", modifier = Modifier.weight(1f))
                    Switch(
                        checked = uiState.useFaceID,
                        onCheckedChange = { checked ->
                            if (checked) {
                                coroutineScope.launch {
                                    val success = viewModel.authenticateWithBiometrics("验证以开启历史保护")
                                    if (success) {
                                        viewModel.setUseFaceID(true)
                                    }
                                }
                            } else {
                                coroutineScope.launch {
                                    val success = viewModel.authenticateWithBiometrics("验证以关闭历史保护")
                                    if (success) {
                                        viewModel.setUseFaceID(false)
                                    }
                                }
                            }
                        }
                    )
                }
            }
            
            // ── Notifications ──
            SettingsSectionHeader("通知")
            InsetGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("开启每日提醒", modifier = Modifier.weight(1f))
                    Switch(
                        checked = uiState.dailyReminderEnabled,
                        onCheckedChange = { viewModel.setDailyReminderEnabled(it) }
                    )
                }
                if (uiState.dailyReminderEnabled) {
                    InsetDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("提醒时间", modifier = Modifier.weight(1f))
                        Text(String.format("%02d:%02d", uiState.reminderHour, uiState.reminderMinute))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Disclaimer Section ──
            SettingsSectionHeader("免责声明与服务协议")
            InsetGroup {
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
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Danger Zone ──
            InsetGroup {
                TextButton(
                    onClick = { showClearConfirmation = true },
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Text("清除所有数据", color = MaterialTheme.colorScheme.error)
                }
            }

            // ── About ──
            SettingsSectionHeader("关于")
            InsetGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("版本", modifier = Modifier.weight(1f))
                    Text("1.0.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

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


@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 32.dp, top = 24.dp, bottom = 8.dp)
    )
}
