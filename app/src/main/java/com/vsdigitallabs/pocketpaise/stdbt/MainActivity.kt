@file:Suppress("DEPRECATION")
package com.vsdigitallabs.pocketpaise.stdbt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vsdigitallabs.pocketpaise.stdbt.ui.navigation.Screen
import com.vsdigitallabs.pocketpaise.stdbt.ui.screens.*
import com.vsdigitallabs.pocketpaise.stdbt.ui.theme.PocketPaiseTheme
import com.vsdigitallabs.pocketpaise.stdbt.viewmodel.PocketPaiseViewModel
import com.vsdigitallabs.pocketpaise.stdbt.viewmodel.PocketPaiseViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PocketPaiseViewModel = viewModel(
                factory = PocketPaiseViewModelFactory(application)
            )
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            PocketPaiseTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Bottom bar visibility control
                val showBottomBar = currentRoute in listOf(
                    Screen.Dashboard.route,
                    Screen.Transactions.route,
                    Screen.Budget.route,
                    Screen.Reports.route,
                    Screen.Profile.route
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp,
                                modifier = Modifier
                                    .testTag("bottom_nav")
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Dashboard.route,
                                    onClick = {
                                        navController.navigate(Screen.Dashboard.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home") },
                                    alwaysShowLabel = false,
                                    modifier = Modifier.testTag("nav_home")
                                )

                                NavigationBarItem(
                                    selected = currentRoute == Screen.Transactions.route,
                                    onClick = {
                                        navController.navigate(Screen.Transactions.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Transactions") },
                                    label = { Text("Transactions") },
                                    alwaysShowLabel = false,
                                    modifier = Modifier.testTag("nav_transactions")
                                )

                                NavigationBarItem(
                                    selected = currentRoute == Screen.Budget.route,
                                    onClick = {
                                        navController.navigate(Screen.Budget.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.AddModerator, contentDescription = "Budget") },
                                    label = { Text("Budget") },
                                    alwaysShowLabel = false,
                                    modifier = Modifier.testTag("nav_budget")
                                )

                                NavigationBarItem(
                                    selected = currentRoute == Screen.Reports.route,
                                    onClick = {
                                        navController.navigate(Screen.Reports.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Reports") },
                                    label = { Text("Reports") },
                                    alwaysShowLabel = false,
                                    modifier = Modifier.testTag("nav_reports")
                                )

                                NavigationBarItem(
                                    selected = currentRoute == Screen.Profile.route,
                                    onClick = {
                                        navController.navigate(Screen.Profile.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                    label = { Text("Profile") },
                                    alwaysShowLabel = false,
                                    modifier = Modifier.testTag("nav_profile")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Splash.route,
                        modifier = Modifier.padding(
                            bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp
                        )
                    ) {
                        composable(Screen.Splash.route) {
                            SplashScreen(navController, viewModel)
                        }
                        composable(Screen.Login.route) {
                            LoginScreen(navController, viewModel)
                        }
                        composable(Screen.Register.route) {
                            RegisterScreen(navController, viewModel)
                        }
                        composable(Screen.ForgotPassword.route) {
                            ForgotPasswordScreen(navController, viewModel)
                        }
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(navController, viewModel)
                        }
                        composable(Screen.Transactions.route) {
                            TransactionsScreen(navController, viewModel)
                        }
                        composable(Screen.Budget.route) {
                            BudgetScreen(navController, viewModel)
                        }
                        composable(Screen.Savings.route) {
                            SavingsScreen(navController, viewModel)
                        }
                        composable(Screen.Reports.route) {
                            ReportsScreen(navController, viewModel)
                        }
                        composable(Screen.Calendar.route) {
                            CalendarScreen(navController, viewModel)
                        }
                        composable(Screen.Notification.route) {
                            NotificationScreen(navController, viewModel)
                        }
                        composable(Screen.Profile.route) {
                            ProfileScreen(navController, viewModel)
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(navController, viewModel)
                        }
                        composable(Screen.PrivacyPolicy.route) {
                            PrivacyPolicyScreen(navController)
                        }
                        composable(Screen.Terms.route) {
                            TermsAndConditionsScreen(navController)
                        }
                        composable(Screen.About.route) {
                            AboutAppScreen(navController)
                        }
                        composable(Screen.Contact.route) {
                            ContactSupportScreen(navController)
                        }
                    }
                }
            }
        }
    }
}
