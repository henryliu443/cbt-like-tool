package com.henryliu.cbtreframe.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.henryliu.cbtreframe.shared.viewmodels.MoodInsightsViewModel
import com.henryliu.cbtreframe.shared.viewmodels.TimeFilter
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodInsightsView(viewModel: MoodInsightsViewModel = koinInject<MoodInsightsViewModel>()) {
    DisposableEffect(Unit) { onDispose { viewModel.clear() } }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mood Insights") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilterChip(
                    selected = uiState.timeFilter == TimeFilter.DAYS_7,
                    onClick = { viewModel.setTimeFilter(TimeFilter.DAYS_7) },
                    label = { Text("7 Days") }
                )
                FilterChip(
                    selected = uiState.timeFilter == TimeFilter.DAYS_30,
                    onClick = { viewModel.setTimeFilter(TimeFilter.DAYS_30) },
                    label = { Text("30 Days") }
                )
                FilterChip(
                    selected = uiState.timeFilter == TimeFilter.DAYS_90,
                    onClick = { viewModel.setTimeFilter(TimeFilter.DAYS_90) },
                    label = { Text("90 Days") }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally))
            } else if (uiState.dataPoints.isEmpty()) {
                Text(
                    "No mood data for this period",
                    modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn {
                    items(uiState.dataPoints) { point ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(point.dateLabel, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Avg Intensity: ${(point.avgIntensity * 10).roundToInt() / 10.0}")
                                    Text("Reframe Count: ${point.reframeFrequency}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
