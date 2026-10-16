package com.tigermatrix.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tigermatrix.app.MainActivity
import com.tigermatrix.app.ui.screens.AssistantScreen
import com.tigermatrix.app.ui.screens.DashboardScreen
import com.tigermatrix.app.ui.screens.NotesScreen
import com.tigermatrix.app.ui.screens.OnboardingScreen
import com.tigermatrix.app.ui.screens.PermissionsScreen
import com.tigermatrix.app.ui.screens.SettingsScreen
import com.tigermatrix.app.ui.screens.TasksScreen
import com.tigermatrix.app.ui.screens.ToolsScreen
import com.tigermatrix.app.ui.theme.TigerMatrixTheme
import com.tigermatrix.app.viewmodel.SettingsViewModel

@Composable
fun TigerMatrixApp(launchAction: String?, onLaunchActionHandled: () -> Unit) {
    val settingsVm: SettingsViewModel = viewModel()
    val themeMode by settingsVm.themeMode.collectAsStateWithLifecycle()
    val onboarded by settingsVm.onboarded.collectAsStateWithLifecycle()

    TigerMatrixTheme(themeMode) {
        // الواجهة كلها من اليمين لليسار
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            when (onboarded) {
                null -> Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {}
                false -> OnboardingScreen(onFinish = { settingsVm.setOnboarded() })
                true -> MainShell(settingsVm, launchAction, onLaunchActionHandled)
            }
        }
    }
}

private fun NavHostController.navigateTop(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun MainShell(
    settingsVm: SettingsViewModel,
    launchAction: String?,
    onLaunchActionHandled: () -> Unit
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentDestination = backStack?.destination
    val showBar = Dest.values().any { d -> currentDestination?.hierarchy?.any { it.route == d.route } == true }

    var openCreateTask by remember { mutableStateOf(false) }
    var openCreateNote by remember { mutableStateOf(false) }

    // أوامر الويدجت
    LaunchedEffect(launchAction) {
        when (launchAction) {
            MainActivity.ACTION_ADD_TASK -> {
                nav.navigateTop(Dest.Tasks.route)
                openCreateTask = true
            }
            MainActivity.ACTION_ADD_NOTE -> {
                nav.navigateTop(Dest.Notes.route)
                openCreateNote = true
            }
            MainActivity.ACTION_ASSISTANT -> nav.navigateTop(Dest.Assistant.route)
        }
        if (launchAction != null) onLaunchActionHandled()
    }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    Dest.values().forEach { d ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == d.route } == true,
                            onClick = { nav.navigateTop(d.route) },
                            icon = { Icon(d.icon, contentDescription = d.label) },
                            label = { Text(d.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Dest.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Dest.Dashboard.route) {
                DashboardScreen(onOpenSettings = { nav.navigate(Dest.SETTINGS_ROUTE) })
            }
            composable(Dest.Notes.route) {
                NotesScreen(openCreate = openCreateNote, onCreateHandled = { openCreateNote = false })
            }
            composable(Dest.Tasks.route) {
                TasksScreen(openCreate = openCreateTask, onCreateHandled = { openCreateTask = false })
            }
            composable(Dest.Assistant.route) { AssistantScreen(settingsVm = settingsVm) }
            composable(Dest.Tools.route) { ToolsScreen() }
            composable(Dest.SETTINGS_ROUTE) {
                SettingsScreen(
                    vm = settingsVm,
                    onBack = { nav.popBackStack() },
                    onOpenPermissions = { nav.navigate(Dest.PERMISSIONS_ROUTE) }
                )
            }
            composable(Dest.PERMISSIONS_ROUTE) {
                PermissionsScreen(vm = settingsVm, onBack = { nav.popBackStack() })
            }
        }
    }
}
