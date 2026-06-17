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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.henryliu.cbtreframe.shared.AIProvider
import com.henryliu.cbtreframe.shared.GlobalSettings
import com.henryliu.cbtreframe.shared.SettingsViewModel
import com.henryliu.cbtreframe.shared.ThinkingTemplate
import com.henryliu.cbtreframe.shared.ThinkingTemplate.AnalysisDepth
import com.henryliu.cbtreframe.shared.ThinkingTemplate.AppResponseStyle
import com.henryliu.cbtreframe.shared.requiresApiKey
import com.henryliu.cbtreframe.shared.defaultModelId
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    globalSettings: GlobalSettings,
    onGlobalSettingsChange: (GlobalSettings) -> Unit,
    onReadDisclaimerClick: () -> Unit,
    onClearDatabase: suspend () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showDisableDisclaimerConfirm by remember { mutableStateOf(false) }
    var showFaceIDDisableBlockedAlert by remember { mutableStateOf(false) }
    var showKeyField by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val versionName = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
    
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
                ),
                actions = {
                    if (uiState.isRefreshingModels) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "获取模型中",
                                style = MaterialTheme.typography.bodySmall,
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
            
            // ── AI Provider ──
            SettingsSectionHeader("AI 服务商", Icons.Default.Memory)
            InsetGroup {
                AIProvider.entries.forEach { provider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                println("UI_CLICK: SettingsScreen Provider clicked: ${provider.name}")
                                viewModel.selectProvider(provider) 
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(provider.displayName(), style = MaterialTheme.typography.bodyLarge)
                            if (!provider.requiresApiKey()) {
                                Text(
                                    "无需 API Key，离线可用",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (uiState.selectedProvider == provider) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (provider != AIProvider.entries.last()) {
                        InsetDivider()
                    }
                }
            }
            
            // ── API Key & Model ──
            if (uiState.selectedProvider != AIProvider.LOCAL) {
                SettingsSectionHeader("API 设置", Icons.Default.VpnKey)
                InsetGroup {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = uiState.apiKeyInput,
                            onValueChange = { viewModel.updateApiKeyInput(it) },
                            label = { Text("API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showKeyField) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showKeyField = !showKeyField }) {
                                    Icon(
                                        imageVector = if (showKeyField) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showKeyField) "隐藏" else "显示"
                                    )
                                }
                            }
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
                        if (uiState.hasAPIKey) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Secure",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "已安全加密存储在系统 KeyStore 保护区",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (uiState.isRefreshingModels && uiState.hasAPIKey) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "正在从服务商获取可用模型…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = "Key 仅存储在你设备的 KeyStore 本地加密区中，不会上传到任何服务器。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))

            SettingsSectionHeader("模型选择", Icons.Default.Category)
            InsetGroup {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (uiState.isRefreshingModels) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("正在获取模型列表", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("请稍候，完成后可在此选择模型", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    
                    if (uiState.resolvedModels.isEmpty()) {
                        Text("无可用模型", color = MaterialTheme.colorScheme.error)
                    } else {
                        uiState.resolvedModels.forEach { model ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !uiState.isRefreshingModels) { viewModel.selectModel(model.modelName) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.selectedModelId == model.modelName,
                                    onClick = { viewModel.selectModel(model.modelName) },
                                    enabled = !uiState.isRefreshingModels
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    model.displayName,
                                    modifier = Modifier.weight(1f),
                                    color = if (uiState.isRefreshingModels) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    
                    if (uiState.selectedProvider != AIProvider.LOCAL) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = { coroutineScope.launch { viewModel.refreshModels() } },
                                enabled = !uiState.isRefreshingModels && uiState.hasAPIKey
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("刷新模型列表")
                            }
                        }
                    }
                }
            }
            
            val modelFooterText = when {
                uiState.isRefreshingModels -> "正在连接服务商并拉取当前账号可用的模型，完成后列表会自动更新。"
                uiState.modelsListError != null -> uiState.modelsListError ?: ""
                uiState.selectedProvider != AIProvider.LOCAL -> "保存 API Key 后会自动从服务商拉取最新可用模型并缓存在本机；也可手动刷新。拉取失败时使用内置备选列表。"
                else -> ""
            }
            if (modelFooterText.isNotEmpty()) {
                SettingsSectionFooter(modelFooterText)
            }

            // ── Preferences (Analysis Depth) ──
            SettingsSectionHeader("分析深度", Icons.Default.Tune)
            InsetGroup {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    AnalysisDepth.entries.forEach { depth ->
                        val isSelected = globalSettings.analysisDepth == depth
                        val icon = when (depth) {
                            AnalysisDepth.fast -> Icons.Default.FlashOn
                            AnalysisDepth.balanced -> Icons.Default.Tune
                            AnalysisDepth.deep -> Icons.Default.Psychology
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onGlobalSettingsChange(globalSettings.copy(analysisDepth = depth)) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(depth.displayName(), style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    depth.description(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (depth != AnalysisDepth.entries.last()) {
                            InsetDivider()
                        }
                    }
                }
            }
            
            // ── App Response Style ──
            SettingsSectionHeader("回应风格", Icons.Default.ChatBubble)
            InsetGroup {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppResponseStyle.entries.forEach { style ->
                            val isSelected = globalSettings.responseStyle == style
                            FilterChip(
                                selected = isSelected,
                                onClick = { onGlobalSettingsChange(globalSettings.copy(responseStyle = style)) },
                                label = { Text(style.displayName()) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = globalSettings.responseStyle.description(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // ── Thinking Template ──
            SettingsSectionHeader("思维模板", Icons.Default.Description)
            InsetGroup {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    ThinkingTemplate.entries.forEach { template ->
                        val isSelected = globalSettings.thinkingTemplate == template
                        val icon = when (template) {
                            ThinkingTemplate.cbt -> Icons.Default.Autorenew
                            ThinkingTemplate.socratic -> Icons.Default.QuestionAnswer
                            ThinkingTemplate.behavioral -> Icons.AutoMirrored.Filled.DirectionsRun
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onGlobalSettingsChange(globalSettings.copy(thinkingTemplate = template)) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(template.displayName(), style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    template.description(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (template != ThinkingTemplate.entries.last()) {
                            InsetDivider()
                        }
                    }
                }
            }
            
            // ── Privacy & Security ──
            SettingsSectionHeader("隐私与安全", Icons.Default.Security)
            InsetGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("生物识别保护历史记录 (Biometrics)", modifier = Modifier.weight(1f))
                    Switch(
                        checked = uiState.useFaceID,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                if (checked) {
                                    val success = viewModel.authenticateWithBiometrics("验证以开启历史记录保护")
                                    if (success) {
                                        viewModel.setUseFaceID(true)
                                    } else {
                                        Toast.makeText(context, "验证失败，无法修改设置", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    val success = viewModel.authenticateWithBiometrics("验证以关闭历史记录保护")
                                    if (success) {
                                        viewModel.setUseFaceID(false)
                                    } else {
                                        showFaceIDDisableBlockedAlert = true
                                    }
                                }
                            }
                        }
                    )
                }
            }
            
            // ── Notifications ──
            SettingsSectionHeader("通知", Icons.Default.Notifications)
            InsetGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
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
                            .clickable {
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        viewModel.setReminderHour(hourOfDay)
                                        viewModel.setReminderMinute(minute)
                                    },
                                    uiState.reminderHour,
                                    uiState.reminderMinute,
                                    true
                                ).show()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.width(36.dp))
                        Text("提醒时间", modifier = Modifier.weight(1f))
                        Text(String.format("%02d:%02d", uiState.reminderHour, uiState.reminderMinute))
                    }
                }
            }
            SettingsSectionFooter("每日提醒通过系统本地通知发送。请在系统设置里允许通知。")

            // ── Disclaimer Section ──
            SettingsSectionHeader("免责声明与服务协议", Icons.Default.Warning)
            InsetGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onReadDisclaimerClick() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "阅读完整免责声明与服务协议",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "查看",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                
                InsetDivider()

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
            SettingsSectionHeader("危险区域", Icons.Default.Delete)
            InsetGroup {
                TextButton(
                    onClick = { showClearConfirmation = true },
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Text("清除所有数据", color = MaterialTheme.colorScheme.error)
                }
            }

            // ── About ──
            SettingsSectionHeader("关于", Icons.Default.Info)
            InsetGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("版本", modifier = Modifier.weight(1f))
                    Text(versionName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                InsetDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("当前服务商", modifier = Modifier.weight(1f))
                    Text(uiState.selectedProvider.displayName(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                InsetDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("当前模型", modifier = Modifier.weight(1f))
                    val currentModelName = uiState.selectedModel.displayName.ifEmpty { null }
                        ?: uiState.selectedModel.modelName.ifEmpty { null }
                        ?: uiState.selectedModelId.ifEmpty { null }
                        ?: uiState.selectedProvider.defaultModelId()
                    Text(currentModelName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            SettingsSectionFooter("在「历史」可搜索与收藏记录，并导出当前列表为 JSON（V2.1）。")

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
                        viewModel.clearAllData(onClearDatabase)
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
                        (context as? android.app.Activity)?.finishAffinity()
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

    if (showFaceIDDisableBlockedAlert) {
        AlertDialog(
            onDismissRequest = { showFaceIDDisableBlockedAlert = false },
            title = { Text("无法关闭历史记录保护") },
            text = { Text("请先通过生物识别验证，才能关闭历史记录保护。") },
            confirmButton = {
                TextButton(onClick = { showFaceIDDisableBlockedAlert = false }) {
                    Text("我知道了")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 32.dp, top = 24.dp, bottom = 8.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSectionFooter(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 16.dp),
        lineHeight = 16.sp
    )
}
