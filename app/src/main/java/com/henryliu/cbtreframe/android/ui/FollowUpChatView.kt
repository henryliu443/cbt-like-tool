package com.henryliu.cbtreframe.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.henryliu.cbtreframe.shared.FollowUpMessage
import com.henryliu.cbtreframe.shared.viewmodels.HistoryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowUpChatView(
    entryId: String,
    originalThought: String,
    lastConclusion: String,
    templateRaw: String,
    providerRaw: String,
    modelRaw: String,
    existingMessagesJSON: String,
    viewModel: HistoryViewModel,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var messages by remember {
        mutableStateOf<List<FollowUpMessage>>(emptyList())
    }
    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    LaunchedEffect(existingMessagesJSON) {
        if (existingMessagesJSON.isNotBlank()) {
            try {
                messages = kotlinx.serialization.json.Json.decodeFromString(existingMessagesJSON)
            } catch (e: Exception) {
                messages = listOf(
                    FollowUpMessage(role = "assistant", text = "你刚刚的原始想法：$originalThought"),
                    FollowUpMessage(role = "assistant", text = "上一轮结论：$lastConclusion")
                )
            }
        } else {
            messages = listOf(
                FollowUpMessage(role = "assistant", text = "你刚刚的原始想法：$originalThought"),
                FollowUpMessage(role = "assistant", text = "上一轮结论：$lastConclusion")
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("继续探索", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                reverseLayout = false
            ) {
                item { Spacer(Modifier.height(16.dp)) }
                items(messages) { msg ->
                    ChatBubble(msg)
                }
                if (isSending) {
                    item {
                        Text(
                            text = "回复中...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("继续追问...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isSending) {
                                val text = inputText
                                inputText = ""
                                isSending = true
                                messages = messages + FollowUpMessage(role = "user", text = text)
                                scope.launch {
                                    try {
                                        val aiMsg = viewModel.sendFollowUpMessage(
                                            entryId = entryId,
                                            originalThought = originalThought,
                                            lastConclusion = lastConclusion,
                                            messages = messages.dropLast(1),
                                            newText = text,
                                            templateRaw = templateRaw,
                                            providerRaw = providerRaw,
                                            modelRaw = modelRaw
                                        )
                                        messages = messages + aiMsg
                                    } catch (e: Exception) {
                                        val errorMsg = (e as? com.henryliu.cbtreframe.shared.AIServiceError)?.userFacingMessage ?: e.message ?: "Unknown error"
                                        messages = messages + FollowUpMessage(role = "assistant", text = errorMsg)
                                    } finally {
                                        isSending = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                            .size(48.dp),
                        enabled = !isSending && inputText.isNotBlank()
                    ) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = "发送",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: FollowUpMessage) {
    val isUser = message.role == "user"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentWidth(if (isUser) Alignment.End else Alignment.Start)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
