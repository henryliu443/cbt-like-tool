package com.henryliu.cbtreframe

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import com.henryliu.cbtreframe.shared.ReframeViewModel
import com.henryliu.cbtreframe.shared.HistoryViewModel
import com.henryliu.cbtreframe.shared.AIModel
import com.henryliu.cbtreframe.shared.db.HistoryEntity

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Reframe") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("History") })
        }
        
        Box(modifier = Modifier.weight(1f)) {
            Crossfade(targetState = selectedTab, label = "tab_transition") { tab ->
                if (tab == 0) {
                    val reframeViewModel: ReframeViewModel = koinInject()
                    ReframeScreen(reframeViewModel)
                } else {
                    val historyViewModel: HistoryViewModel = koinInject()
                    HistoryScreen(historyViewModel)
                }
            }
        }
    }
}

@Composable
fun ReframeScreen(viewModel: ReframeViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var input by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .animateContentSize()
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Enter thought") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.reframe(input, AIModel.GPT_4O) },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reframe")
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        AnimatedVisibility(
            visible = uiState.isLoading && uiState.response.isEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CircularProgressIndicator()
        }
        
        AnimatedVisibility(
            visible = uiState.response.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(text = uiState.response)
        }

        if (uiState.error != null) {
            Text(text = "Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val historyList by viewModel.history.collectAsState()

    if (historyList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("No history yet.")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp).animateContentSize()) {
            items(items = historyList, key = { it.id }) { item ->
                Box {
                HistoryItemCard(item, onDelete = { viewModel.deleteItem(it) })
                Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(item: HistoryEntity, onDelete: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Original: ${item.originalThought}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Reframed: ${item.reframedThought}", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Model: ${item.modelName}", style = MaterialTheme.typography.labelSmall)
                Button(onClick = { onDelete(item.id) }) {
                    Text("Delete")
                }
            }
        }
    }
}
