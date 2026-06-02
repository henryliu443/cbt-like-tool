package com.henryliu.cbtreframe.android.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.henryliu.cbtreframe.shared.db.HistoryEntity
import com.henryliu.cbtreframe.shared.viewmodels.HistoryViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryView(viewModel: HistoryViewModel = koinInject<HistoryViewModel>()) {
    val context = LocalContext.current
    var isUnlocked by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    val uiState by viewModel.uiState.collectAsState()
    val entries by viewModel.history.collectAsState()

    LaunchedEffect(Unit) {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        
        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor = ContextCompat.getMainExecutor(context)
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock History")
                    .setSubtitle("Authenticate to view your history")
                    .setAllowedAuthenticators(authenticators)
                    .build()
                    
                val activity = context as? FragmentActivity
                if (activity != null) {
                    val biometricPrompt = BiometricPrompt(
                        activity, 
                        executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                authError = "Authentication error: $errString"
                            }
    
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                isUnlocked = true
                            }
    
                            override fun onAuthenticationFailed() {
                                super.onAuthenticationFailed()
                                authError = "Authentication failed"
                            }
                        }
                    )
                    biometricPrompt.authenticate(promptInfo)
                } else {
                    authError = "Activity context required for biometric prompt"
                }
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // Extreme fallback if no password
                isUnlocked = true
            }
            else -> {
                isUnlocked = true
            }
        }
    }

    if (!isUnlocked) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(authError ?: "Authenticating...", color = MaterialTheme.colorScheme.onSurface)
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史记录") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = uiState.searchText,
                onValueChange = { viewModel.setSearchText(it) },
                label = { Text("搜索") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (entries.isEmpty() && uiState.searchText.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "还没有历史记录",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.groupedEntries.forEach { group ->
                        item(key = "header_${group.dateLabel}") {
                            Text(
                                text = group.dateLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                            )
                        }
                        items(
                            count = group.entries.size,
                            key = { index -> group.entries[index].id }
                        ) { index ->
                            HistoryEntryCard(
                                entry = group.entries[index],
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(
    entry: HistoryEntity,
    viewModel: HistoryViewModel,
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = entry.inputText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(8.dp))
            
            Text(
                text = HistoryViewModel.formatTimestamp(entry.timestamp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    
                    Text(
                        text = entry.aiResponse ?: "无回复",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("删除此记录", fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("此操作不可撤销。确定要删除这条记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteItem(entry.id)
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}
