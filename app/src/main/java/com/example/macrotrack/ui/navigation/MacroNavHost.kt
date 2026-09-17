package com.example.macrotrack.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.macrotrack.data.datastore.UserProfile
import com.example.macrotrack.ui.camera.CameraScreen
import com.example.macrotrack.ui.dashboard.DashboardScreen
import com.example.macrotrack.ui.history.HistoryScreen
import com.example.macrotrack.ui.onboarding.OnboardingScreen
import com.example.macrotrack.util.AppViewModelFactory

private sealed class Destination(val route: String, val label: String) {
    data object Dashboard : Destination("dashboard", "Aujourd'hui")
    data object Camera : Destination("camera", "Camera")
    data object History : Destination("history", "Historique")
}

private val bottomDestinations = listOf(Destination.Dashboard, Destination.Camera, Destination.History)

@Composable
fun MacroNavHost(factory: AppViewModelFactory, profile: UserProfile) {
    if (!profile.onboardingDone) {
        OnboardingScreen(factory = factory, onDone = { /* recomposition automatique via le Flow du profil */ })
        return
    }

    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            NavigationBar {
                bottomDestinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (destination) {
                                    Destination.Dashboard -> Icons.Filled.Home
                                    Destination.Camera -> Icons.Filled.CameraAlt
                                    Destination.History -> Icons.Filled.History
                                },
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Destination.Dashboard.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Destination.Dashboard.route) { DashboardScreen(factory) }
            composable(Destination.Camera.route) { CameraScreen(factory) }
            composable(Destination.History.route) { HistoryScreen(factory) }
        }
    }
}
