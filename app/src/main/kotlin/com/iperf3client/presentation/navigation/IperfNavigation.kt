package com.iperf3client.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.iperf3client.R
import com.iperf3client.presentation.screen.home.HomeScreen
import com.iperf3client.presentation.screen.servers.ServersScreen
import com.iperf3client.presentation.screen.history.HistoryScreen
import com.iperf3client.presentation.screen.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IperfNavigation(
    windowSizeClass: WindowSizeClass,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val useNavigationDrawer = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    
    if (useNavigationDrawer) {
        // Large screens - use navigation drawer
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet {
                    NavigationDrawerContent(
                        currentDestination = currentDestination,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) {
            IperfNavHost(
                navController = navController,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        // Small/Medium screens - use bottom navigation
        Scaffold(
            bottomBar = {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.titleRes)) },
                            selected = currentDestination?.hierarchy?.any { 
                                it.route == item.route 
                            } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            IperfNavHost(
                navController = navController,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

@Composable
private fun NavigationDrawerContent(
    currentDestination: androidx.navigation.NavDestination?,
    onNavigate: (String) -> Unit
) {
    bottomNavItems.forEach { item ->
        NavigationDrawerItem(
            icon = { Icon(item.icon, contentDescription = null) },
            label = { Text(stringResource(item.titleRes)) },
            selected = currentDestination?.hierarchy?.any { 
                it.route == item.route 
            } == true,
            onClick = { onNavigate(item.route) }
        )
    }
}

@Composable
private fun IperfNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen()
        }
        composable(Screen.Servers.route) {
            ServersScreen()
        }
        composable(Screen.History.route) {
            HistoryScreen(
                onViewDetails = { testResult ->
                    // TODO: Navigate to detailed results screen
                },
                onExportCsv = { testResult ->
                    // TODO: Handle CSV export
                },
                onExportJson = { testResult ->
                    // TODO: Handle JSON export
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Servers : Screen("servers")
    object History : Screen("history")
    object Settings : Screen("settings")
}

data class BottomNavItem(
    val route: String,
    val titleRes: Int,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(
        route = Screen.Home.route,
        titleRes = R.string.nav_home,
        icon = Icons.Default.Home
    ),
    BottomNavItem(
        route = Screen.Servers.route,
        titleRes = R.string.nav_servers,
        icon = Icons.Default.Storage
    ),
    BottomNavItem(
        route = Screen.History.route,
        titleRes = R.string.nav_history,
        icon = Icons.Default.History
    ),
    BottomNavItem(
        route = Screen.Settings.route,
        titleRes = R.string.nav_settings,
        icon = Icons.Default.Settings
    )
)