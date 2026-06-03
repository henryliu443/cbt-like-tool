package com.henryliu.cbtreframe

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.koin.compose.koinInject
import com.henryliu.cbtreframe.shared.ReframeViewModel
import com.henryliu.cbtreframe.shared.viewmodels.HistoryViewModel
import com.henryliu.cbtreframe.shared.SettingsViewModel
import com.henryliu.cbtreframe.shared.GlobalSettings
import com.henryliu.cbtreframe.android.ui.HomeView
import com.henryliu.cbtreframe.ui.SettingsScreen
import com.henryliu.cbtreframe.ui.OnboardingScreen
import com.henryliu.cbtreframe.android.ui.HistoryView
import com.henryliu.cbtreframe.android.ui.ThoughtJournalView
import com.henryliu.cbtreframe.android.ui.MoodInsightsView
import com.henryliu.cbtreframe.android.ui.ExercisesView
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Build

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val settingsViewModel: SettingsViewModel = koinInject()
    val uiState by settingsViewModel.uiState.collectAsState()

    // ── Global onboarding / disclaimer gate ──
    // Blocks the entire app until the user completes onboarding and accepts
    // the disclaimer.  The flag is persisted in SettingsManager.
    if (!uiState.hasAcceptedDisclaimer) {
        OnboardingScreen(
            viewModel = settingsViewModel,
            onOnboardingComplete = {
                // The OnboardingScreen itself calls setHasAcceptedDisclaimer(true)
                // via the "完成" button in StepDisclaimer.  This callback just
                // allows the composition to recompose and show the main app.
            },
        )
        return
    }

    // ── Normal app (disclaimer accepted) ──
    val navController = rememberNavController()

    val globalSettingsSaver = androidx.compose.runtime.saveable.Saver<GlobalSettings, String>(
        save = { kotlinx.serialization.json.Json.encodeToString(GlobalSettings.serializer(), it) },
        restore = { kotlinx.serialization.json.Json.decodeFromString(GlobalSettings.serializer(), it) }
    )
    var globalSettings by androidx.compose.runtime.saveable.rememberSaveable(stateSaver = globalSettingsSaver) { mutableStateOf(GlobalSettings.Default) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("首页") },
                    selected = currentRoute == "home",
                    onClick = {
                        if (currentRoute != "home") {
                            navController.navigate("home") {
                                popUpTo(navController.graph.startDestinationId) { 
                                    saveState = true
                                    inclusive = false 
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Create, contentDescription = "Records") },
                    label = { Text("记录") },
                    selected = currentRoute == "records",
                    onClick = {
                        if (currentRoute != "records") {
                            navController.navigate("records") { 
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "History") },
                    label = { Text("历史") },
                    selected = currentRoute == "history",
                    onClick = {
                        if (currentRoute != "history") {
                            navController.navigate("history") { 
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = "Trends") },
                    label = { Text("趋势") },
                    selected = currentRoute == "trends",
                    onClick = {
                        if (currentRoute != "trends") {
                            navController.navigate("trends") { 
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Build, contentDescription = "Practice") },
                    label = { Text("Practice") },
                    selected = currentRoute == "practice",
                    onClick = {
                        if (currentRoute != "practice") {
                            navController.navigate("practice") { 
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.MoreVert, contentDescription = "More") },
                    label = { Text("更多") },
                    selected = currentRoute == "settings",
                    onClick = {
                        if (currentRoute != "settings") {
                            navController.navigate("settings") { 
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                val reframeViewModel: ReframeViewModel = koinInject()
                val historyViewModel: HistoryViewModel = koinInject()
                val history by historyViewModel.history.collectAsState()
                HomeView(
                    viewModel = reframeViewModel,
                    globalSettings = globalSettings,
                    recentHistory = history.sortedByDescending { it.timestamp },
                    onTemplateChanged = { newTemplate -> globalSettings = globalSettings.copy(thinkingTemplate = newTemplate) }
                )
            }
            composable("records") {
                ThoughtJournalView()
            }
            composable("history") {
                HistoryView()
            }
            composable("trends") {
                MoodInsightsView()
            }
            composable("practice") {
                ExercisesView()
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    globalSettings = globalSettings,
                    onGlobalSettingsChange = { globalSettings = it }
                )
            }
        }
    }
}
