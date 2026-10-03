package com.example.filetransfer.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.filetransfer.ui.connection.ConnectionScreen
import com.example.filetransfer.ui.history.HistoryScreen
import com.example.filetransfer.ui.root.IncomingTransferHost
import com.example.filetransfer.ui.transfer.TransferScreen
import com.example.filetransfer.ui.transfer.TransferViewModel

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    onRequestPermission: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Global Incoming Transfer Request Dialog (PRD Assumption #1)
    IncomingTransferHost(
        onAccepted = { peerName ->
            navController.navigate(Screen.Transfer.createRoute(peerName)) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Discovery.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Discovery.route) {
                ConnectionScreen(
                    onNavigateToTransfer = { peerName ->
                        navController.navigate(Screen.Transfer.createRoute(peerName))
                    },
                    onRequestPermission = onRequestPermission
                )
            }

            composable(
                route = Screen.Transfer.route,
                arguments = listOf(
                    navArgument(Screen.Transfer.ARG_PEER_NAME) {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) {
                val transferViewModel: TransferViewModel = hiltViewModel()
                TransferScreen(
                    viewModel = transferViewModel,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen()
            }
        }
    }
}
