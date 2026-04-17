package com.acdcmaia.callguard.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.acdcmaia.callguard.ui.blacklist.BlacklistScreen
import com.acdcmaia.callguard.ui.calls.RecentCallsScreen
import com.acdcmaia.callguard.ui.settings.SettingsScreen

sealed class Screen(val route: String, val label: String) {
    object Calls : Screen("calls", "Chamadas")
    object Blacklist : Screen("blacklist", "Lista Negra")
    object Settings : Screen("settings", "Configurações")
}

@Composable
fun CallGuardNavigation() {
    val navController = rememberNavController()
    val items = listOf(Screen.Calls, Screen.Blacklist, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            when (screen) {
                                Screen.Calls -> Icon(Icons.Default.History, contentDescription = null)
                                Screen.Blacklist -> Icon(Icons.Default.Block, contentDescription = null)
                                Screen.Settings -> Icon(Icons.Default.Settings, contentDescription = null)
                            }
                        },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Calls.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Calls.route) { RecentCallsScreen() }
            composable(Screen.Blacklist.route) { BlacklistScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
