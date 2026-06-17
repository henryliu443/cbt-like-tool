package com.henryliu.cbtreframe.android.ui

import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun ExercisesView() {
    var expandedCardId by remember { mutableStateOf<Int?>(null) }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Exercises", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        ExerciseCard(
            id = 1,
            title = "Breathing Exercise",
            isExpanded = expandedCardId == 1,
            onExpand = { expandedCardId = if (expandedCardId == 1) null else 1 }
        )
    }
}

@Composable
fun ExerciseCard(
    id: Int,
    title: String,
    isExpanded: Boolean,
    onExpand: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpand() }
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Follow the haptic feedback for guided breathing.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    BreathingGuidance()
                }
            }
        }
    }
}

@Composable
fun BreathingGuidance() {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var instruction by remember { mutableStateOf("Ready to start") }

    val vibrator = remember {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    }

    DisposableEffect(Unit) {
        onDispose {
            vibrator.cancel()
        }
    }
    
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                instruction = "Inhale..."
                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
                delay(4000) //TODO：这里我就觉得很不合适 可以参考applewatch正念的逻辑
                
                instruction = "Hold..."
                delay(4000) //TODO：这里我就觉得很不合适 可以参考applewatch正念的逻辑
                
                instruction = "Exhale..."
                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
                delay(4000) //TODO：这里我就觉得很不合适 可以参考applewatch正念的逻辑
                
                instruction = "Hold..."
                delay(4000) //TODO：这里我就觉得很不合适 可以参考applewatch正念的逻辑
            }
        } else {
            instruction = "Ready to start"
            vibrator.cancel()
        }
    }

    Column {
        Text(instruction, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { isPlaying = !isPlaying }) {
            Text(if (isPlaying) "Stop" else "Start Breathing Exercise")
        }
    }
}
